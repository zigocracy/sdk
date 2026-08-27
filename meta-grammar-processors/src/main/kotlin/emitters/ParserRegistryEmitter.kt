package com.zigocracy.sdk.processor.emitters

import com.google.devtools.ksp.processing.CodeGenerator
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.writeTo
import com.zigocracy.sdk.grammar.annotations.Associativity
import com.zigocracy.sdk.processor.GrammarEmitter
import com.zigocracy.sdk.processor.ResolvedGrammar

/**
 * Emits `GeneratedParserRegistry`: lookup tables for prefix, suffix, and infix
 * operators used by the precedence-climbing expression parser.
 *
 * This emitter consumes the shared [ResolvedGrammar].
 */
public class ParserRegistryEmitter(
	private val codeGenerator: CodeGenerator
) : GrammarEmitter {

	override val name: String = "ParserRegistry"

	private companion object {
		const val PACKAGE_NAME = "language.syntax"
		const val OBJECT_NAME = "GeneratedParserRegistry"
	}

	override fun emit(grammar: ResolvedGrammar) {
		val prefixes = grammar.prefixOperators
		val suffixes = grammar.suffixOperators
		val infixes = grammar.infixOperators

		if (prefixes.isEmpty() && suffixes.isEmpty() && infixes.isEmpty()) return

		val rootType = grammar.rootType

		// ── InfixInfo data class ────────────────────────────────────────

		val infixInfoSpec = TypeSpec.classBuilder("InfixInfo")
			.addModifiers(KModifier.INTERNAL, KModifier.DATA)
			.primaryConstructor(
				FunSpec.constructorBuilder()
					.addParameter("precedence", Int::class)
					.addParameter("associativity", Associativity::class)
					.build()
			)
			.addProperty(
				PropertySpec.builder("precedence", Int::class)
					.initializer("precedence")
					.build()
			)
			.addProperty(
				PropertySpec.builder("associativity", Associativity::class)
					.initializer("associativity")
					.build()
			)
			.build()

		// ── prefixOperators set ─────────────────────────────────────────

		val prefixSetType = Set::class.asClassName().parameterizedBy(rootType)
		val prefixSetSpec = PropertySpec.builder("prefixOperators", prefixSetType)
			.addKdoc(
				"""
                Set of all tokens that can appear in prefix position during expression parsing.
                
                Prefix operators appear before their operand: `-x`, `!condition`, `&value`.
                
                Example:
                ```kotlin
                check(TokenKind.Minus in GeneratedParserRegistry.prefixOperators)
                ```
                """.trimIndent()
			)
			.initializer(buildCodeBlock {
				add("setOf(\n")
				withIndent {
					prefixes.forEach { token ->
						add("%M,\n", token.className.member(token.entryName))
					}
				}
				add(")")
			})
			.build()

		// ── suffixOperators set ─────────────────────────────────────────

		val suffixSetType = Set::class.asClassName().parameterizedBy(rootType)
		val suffixSetSpec = PropertySpec.builder("suffixOperators", suffixSetType)
			.addKdoc(
				"""
                Set of all tokens that can appear in suffix (postfix) position during expression parsing.
                
                Suffix operators appear after their operand: `x.*`, `value.?`.
                
                Example:
                ```kotlin
                check(TokenKind.Dot in GeneratedParserRegistry.suffixOperators)
                check(TokenKind.DotQuestionMark in GeneratedParserRegistry.suffixOperators)
                ```
                """.trimIndent()
			)
			.initializer(buildCodeBlock {
				add("setOf(\n")
				withIndent {
					suffixes.forEach { token ->
						add("%M,\n", token.className.member(token.entryName))
					}
				}
				add(")")
			})
			.build()

		// ── infixOperators map ──────────────────────────────────────────

		val infixInfoClassName = ClassName(PACKAGE_NAME, OBJECT_NAME, "InfixInfo")
		val infixMapType = Map::class.asClassName().parameterizedBy(rootType, infixInfoClassName)
		val infixMapSpec = PropertySpec.builder("infixOperators", infixMapType)
			.addKdoc(
				"""
                Map of infix operators to their precedence and associativity information.
                
                Infix operators appear between operands: `x + y`, `a == b`, `p or q`.
                Use this map for precedence-climbing expression parsing.
                
                Example:
                ```kotlin
                val plusInfo = GeneratedParserRegistry.infixOperators[TokenKind.Plus]
                check(plusInfo?.precedence == 50)
                check(plusInfo?.associativity == Associativity.LEFT)
                ```
                """.trimIndent()
			)
			.initializer(buildCodeBlock {
				add("mapOf(\n")
				withIndent {
					infixes.forEach { token ->
						val infix = token.infix!!
						add(
							"%M to InfixInfo(precedence = %L, associativity = %T.%L),\n",
							token.className.member(token.entryName),
							infix.precedence,
							Associativity::class,
							infix.associativity
						)
					}
				}
				add(")")
			})
			.build()

		// ── Object assembly ─────────────────────────────────────────────

		val objectSpec = TypeSpec.objectBuilder(OBJECT_NAME)
			.addKdoc(
				"""
                Registry providing operator position and precedence information for expression parsing.
                
                This registry is automatically generated during compilation. It contains
                lookup tables for precedence-climbing expression parsing combined with
                recursive descent parsing.
                
                ## Contents
                
                - [prefixOperators]: Tokens usable in prefix position (before operand)
                - [suffixOperators]: Tokens usable in suffix position (after operand)
                - [infixOperators]: Tokens usable in infix position with precedence and associativity
                
                @see Prefix
                @see Suffix
                @see Infix
                """.trimIndent()
			)
			.addModifiers(KModifier.INTERNAL)
			.addType(infixInfoSpec)
			.addProperty(prefixSetSpec)
			.addProperty(suffixSetSpec)
			.addProperty(infixMapSpec)
			.build()

		FileSpec.builder(PACKAGE_NAME, OBJECT_NAME)
			.addType(objectSpec)
			.build()
			.writeTo(codeGenerator, aggregating = true)
	}
}
