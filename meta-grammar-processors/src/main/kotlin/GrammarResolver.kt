package com.zigocracy.sdk.processor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ksp.toClassName
import com.zigocracy.sdk.grammar.annotations.*

/**
 * The single KSP [SymbolProcessor] that resolves all grammar annotations into a
 * [ResolvedGrammar] and dispatches it to registered [GrammarEmitter]s.
 *
 * ## Pipeline
 *
 * ```
 * Annotations ──► GrammarResolver ──► ResolvedGrammar ──► Emitter₁
 *                 (validate)                           ──► Emitter₂
 *                                                      ──► Emitter₃
 *                                                      ──► …
 * ```
 *
 * The resolver performs all validation (duplicate symbols, conflicting annotations,
 * missing metadata) and constructs the model exactly once. Emitters receive a fully
 * validated, immutable snapshot and never need to touch KSP APIs.
 *
 * The root type must be an `enum class` annotated with [GrammarRoot]. Annotated enum
 * entries are collected as tokens. Entries without any annotations are treated as
 * having lexical representation only (they still appear in token-order sequences).
 *
 * ## Emitter registration
 *
 * Emitters are supplied at construction time through the [emitters] list. The
 * [GrammarResolverProvider] is responsible for assembling this list, injecting
 * each emitter's own dependencies (CodeGenerator, file paths, etc.) at construction.
 *
 * @param logger KSP logger for error and diagnostic reporting.
 * @param emitters The set of emitters to invoke after successful resolution.
 *
 * @see ResolvedGrammar
 * @see GrammarEmitter
 */
public class GrammarResolver(
	private val logger: KSPLogger,
	private val emitters: List<GrammarEmitter>
) : SymbolProcessor {

	private var resolved = false

	override fun process(resolver: Resolver): List<KSAnnotated> {
		if (resolved) return emptyList()

		// Find all types annotated with @GrammarRoot
		val rootDeclarations = resolver.getSymbolsWithAnnotation(
			GrammarRoot::class.qualifiedName!!
		).filterIsInstance<KSClassDeclaration>().toList()

		val root: KSClassDeclaration = when (rootDeclarations.size) {
			0 -> {
				logger.error("No type annotated with @GrammarRoot found! Annotate your enum root, e.g., `@GrammarRoot enum class TokenKind`.")
				return emptyList()
			}

			1 -> rootDeclarations.first()

			else -> {
				logger.error("Multiple types annotated with @GrammarRoot found: $rootDeclarations")
				return emptyList()
			}
		}

		if (Modifier.ENUM !in root.modifiers) {
			logger.error("@GrammarRoot type '${root.simpleName.asString()}' must be an enum class. Non-enum types cannot define token hierarchies.")
			return emptyList()
		}

		val tokenEntries = resolveEnumEntries(root)

		// ── Phase 1: Extract annotation data from every token entry ─────

		val resolvedTokens = buildResolvedTokens(root, tokenEntries)

		// ── Phase 2: Validate cross-token invariants ────────────────────

		val valid = validateSymbolUniqueness(resolvedTokens)
		if (!valid) {
			resolved = true
			return emptyList()
		}

		// ── Phase 3: Build the model ────────────────────────────────────

		val grammar = ResolvedGrammar(
			rootType = root.toClassName(),
			tokens = resolvedTokens
		)

		// ── Phase 4: Dispatch to all emitters ───────────────────────────

		for (emitter in emitters) {
			logger.info("Running emitter: ${emitter.name}")
			emitter.emit(grammar)
		}

		resolved = true
		return emptyList()
	}

	// ── Annotation extraction ───────────────────────────────────────────

	/**
	 * Reads all lexer and parser annotations from each token entry and
	 * constructs a [ResolvedToken] for it.
	 *
	 * Tokens with no annotations at all are reported as errors and skipped.
	 * Tokens annotated as `@Synthetic` that also carry lexical annotations
	 * are reported as errors and skipped.
	 */
	private fun buildResolvedTokens(
		root: KSClassDeclaration,
		tokenEntries: List<KSClassDeclaration>
	): List<ResolvedToken> = buildList {
		val rootClassName = root.toClassName()
		for (entry in tokenEntries) {
			val resolved = resolveEntry(rootClassName, entry)
			if (resolved != null) add(resolved)
		}
	}

	private fun resolveEntry(
		rootClassName: com.squareup.kotlinpoet.ClassName,
		entry: KSClassDeclaration
	): ResolvedToken? {
		val entryName = entry.simpleName.asString()

		val operatorAnnotations: List<Operator> = entry.getAnnotationsByType()
		val keywordAnnotations: List<Keyword> = entry.getAnnotationsByType()
		val punctuationAnnotations: List<Punctuation> = entry.getAnnotationsByType()
		val syntheticAnnotations: List<Synthetic> = entry.getAnnotationsByType()
		val prefixAnnotations: List<Prefix> = entry.getAnnotationsByType()
		val suffixAnnotations: List<Suffix> = entry.getAnnotationsByType()
		val infixAnnotations: List<Infix> = entry.getAnnotationsByType()

		// Must have at least one kind annotation.
		val hasLexical = operatorAnnotations.isNotEmpty() ||
			keywordAnnotations.isNotEmpty() ||
			punctuationAnnotations.isNotEmpty()
		val hasSynthetic = syntheticAnnotations.isNotEmpty()

		if (!hasLexical && !hasSynthetic) {
			logger.error(
				"$entryName has no @Operator, @Keyword, @Punctuation, or @Synthetic annotation",
				entry
			)
			return null
		}

		// Synthetic tokens must not carry lexical annotations.
		if (hasSynthetic) {
			if (hasLexical) {
				logger.error(
					"""
                    Conflict — $entryName is annotated as @Synthetic
                    but also has @Operator/@Keyword/@Punctuation annotations.
                    Remove the other annotations.
                    """.trimIndent(),
					entry
				)
				return null
			}

			return ResolvedToken(
				className = rootClassName,
				entryName = entryName,
				symbol = null,
				kind = null,
				isPrefix = prefixAnnotations.isNotEmpty(),
				isSuffix = suffixAnnotations.isNotEmpty(),
				infix = infixAnnotations.firstOrNull()?.toResolved(),
				isSynthetic = true
			)
		}

		// Determine the lexical kind and symbol.
		val (kind, symbol) = when {
			operatorAnnotations.isNotEmpty() -> TokenKind.OPERATOR to operatorAnnotations.first().symbol
			keywordAnnotations.isNotEmpty() -> TokenKind.KEYWORD to keywordAnnotations.first().symbol
			else -> TokenKind.PUNCTUATION to punctuationAnnotations.first().symbol
		}

		return ResolvedToken(
			className = rootClassName,
			entryName = entryName,
			symbol = symbol,
			kind = kind,
			isPrefix = prefixAnnotations.isNotEmpty(),
			isSuffix = suffixAnnotations.isNotEmpty(),
			infix = infixAnnotations.firstOrNull()?.toResolved(),
			isSynthetic = false
		)
	}

	// ── Validation ──────────────────────────────────────────────────────

	/**
	 * Ensures no two distinct token entries claim the same symbol string.
	 *
	 * Returns `true` if the grammar is valid, `false` if errors were reported.
	 */
	private fun validateSymbolUniqueness(tokens: List<ResolvedToken>): Boolean {
		val symbolOwners = mutableMapOf<String, ResolvedToken>()
		var valid = true

		for (token in tokens) {
			val symbol = token.symbol ?: continue

			val existing = symbolOwners[symbol]
			if (existing != null && existing.entryName != token.entryName) {
				logger.error(
					"""
                    Conflict — same symbol '$symbol' registered twice:
                    * By ${existing.className.simpleName}.${existing.entryName}
                    * Now by ${token.className.simpleName}.${token.entryName}
                    """.trimIndent()
				)
				valid = false
			} else {
				symbolOwners[symbol] = token
			}
		}

		return valid
	}

	// ── Helpers ──────────────────────────────────────────────────────────

	private fun Infix.toResolved(): ResolvedInfix = ResolvedInfix(
		precedence = this.precedence,
		associativity = this.associativity
	)
}
