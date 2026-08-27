package com.zigocracy.sdk.processor.emitters

import com.google.devtools.ksp.processing.CodeGenerator
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.writeTo
import com.zigocracy.sdk.processor.GrammarEmitter
import com.zigocracy.sdk.processor.ResolvedGrammar
import com.zigocracy.sdk.processor.ResolvedToken

/**
 * Emits `GeneratedLexerRegistry`: a bidirectional map between symbol strings
 * and their corresponding [TokenKind] enum constants.
 *
 * The registry is ordered by symbol length (longest first) then lexicographically,
 * which enables greedy longest-match scanning in the lexer.
 *
 * This emitter consumes the shared [com.zigocracy.sdk.processor.ResolvedGrammar]
 * instead of walking KSP annotations itself.
 */
public class LexerRegistryEmitter(
	private val codeGenerator: CodeGenerator
) : GrammarEmitter {

	override val name: String = "LexerRegistry"

	private companion object {
		const val PACKAGE_NAME = "language.syntax"
		const val OBJECT_NAME = "GeneratedLexerRegistry"
	}

	override fun emit(grammar: ResolvedGrammar) {
		val staticTokens = grammar.staticTokens
		if (staticTokens.isEmpty()) return

		// Sort: longest symbol first, then alphabetically within the same length.
		val sorted = staticTokens
			.sortedWith(compareByDescending<ResolvedToken> { it.symbol!!.length }.thenBy { it.symbol!! })

		val rootType = grammar.rootType

		val stringToTokenType = Map::class.asClassName().parameterizedBy(STRING, rootType)
		val stringToTokenSpec = PropertySpec.builder("stringToToken", stringToTokenType)
			.addKdoc(
				"""
                Maps string symbols to their corresponding [TokenKind] enum constants.
                
                Use this map during lexical analysis to convert source text into tokens. The map keys
                are ordered by length (from longest to shortest), and lexicographically (from A to Z),
                ensuring greedy matching.
                
                Example:
                ```kotlin
                check(GeneratedLexerRegistry.stringToToken[">>"] == TokenKind.RArrow2)
                check(GeneratedLexerRegistry.stringToToken["if"] == TokenKind.KeywordIf)
                ```
                """.trimIndent()
			)
			.initializer(buildCodeBlock {
				add("mapOf(\n")
				withIndent {
					sorted.forEach { token ->
						add("%S to %M,\n", token.symbol, token.className.member(token.entryName))
					}
				}
				add(")")
			})
			.build()

		val tokenToStringType = Map::class.asClassName().parameterizedBy(rootType, STRING)
		val tokenToStringSpec = PropertySpec.builder("tokenToString", tokenToStringType)
			.addKdoc(
				"""
                Maps [TokenKind] enum constants back to their canonical string representations.
                
                Use this map for error reporting, pretty-printing, debugging, or code generation
                where you need the textual form of a token.
                
                Example:
                ```kotlin
                check(GeneratedLexerRegistry.tokenToString[TokenKind.RArrow2] == ">>")
                check(GeneratedLexerRegistry.tokenToString[TokenKind.KeywordIf] == "if")
                ```
                """.trimIndent()
			)
			.initializer(buildCodeBlock {
				add("mapOf(\n")
				withIndent {
					sorted.forEach { token ->
						add("%M to %S,\n", token.className.member(token.entryName), token.symbol)
					}
				}
				add(")")
			})
			.build()

		val maxLength = sorted.maxOf { it.symbol!!.length }
		val maxLengthSpec = PropertySpec.builder("maxTokenCandidateLength", Int::class)
			.addKdoc(
				"""
                The length of the longest token symbol in the registry.
                
                Lexers can use this value to optimize lookahead buffering and avoid unnecessary
                string allocations during tokenization.
                
                Example:
                ```kotlin
                for (length in GeneratedLexerRegistry.maxTokenCandidateLength downTo 1) {
                    val candidate = input.substring(position, min(position + length, input.length))
                    // Try to match candidate...
                }
                ```
                """.trimIndent()
			)
			.initializer("%L", maxLength)
			.build()

		val objectSpec = TypeSpec.objectBuilder(OBJECT_NAME)
			.addKdoc(
				"""
                Registry providing bidirectional mapping between token symbols and their
                corresponding [TokenKind] enum constants.
                
                This registry is automatically generated during compilation. It contains all
                statically-defined tokens in the language — those whose identity is fixed and
                known at compile time.
                
                ## Token Categories
                
                - **Keywords**: Reserved language identifiers such as `if`, `fn`, `const`, `while`
                - **Operators**: Symbolic operators like `>>`, `+=`, `||`, `&&`, `==`
                - **Punctuation**: Structural delimiters and separators including `(`, `)`, `{`, `}`, `;`
                
                Tokens annotated with `@Synthetic` are excluded as they have no lexical representation.
                
                @see Operator
                @see Keyword
                @see Punctuation
                @see Synthetic
                """.trimIndent()
			)
			.addModifiers(KModifier.INTERNAL)
			.addProperty(stringToTokenSpec)
			.addProperty(tokenToStringSpec)
			.addProperty(maxLengthSpec)
			.build()

		FileSpec.builder(PACKAGE_NAME, OBJECT_NAME)
			.addType(objectSpec)
			.build()
			.writeTo(codeGenerator, aggregating = true)
	}
}
