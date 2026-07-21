package net.landless_city.zigocracy.processor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ksp.toClassName
import net.landless_city.zigocracy.grammar.annotations.*

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
 * The root type may be either a `sealed class` or a `sealed interface` annotated with
 * [GrammarRoot]. Concrete `object` subclasses are collected in the same way regardless.
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
				logger.error("No type annotated with @GrammarRoot found! Annotate your sealed root, e.g., `@GrammarRoot sealed class TokenType` or `@GrammarRoot sealed interface TokenType`.")
				return emptyList()
			}

			1 -> rootDeclarations.first()

			else -> {
				logger.error("Multiple types annotated with @GrammarRoot found: $rootDeclarations")
				return emptyList()
			}
		}

		if (Modifier.SEALED !in root.modifiers) {
			logger.error("@GrammarRoot type '${root.simpleName.asString()}' must be sealed (sealed class or sealed interface). Non-sealed types cannot define token hierarchies.")
			return emptyList()
		}

		val tokenObjects = resolveObjectChildrenOfSealed(root)

		// ── Phase 1: Extract annotation data from every token object ────

		val resolvedTokens = buildResolvedTokens(tokenObjects)

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
	 * Reads all lexer and parser annotations from each token object and
	 * constructs a [ResolvedToken] for it.
	 *
	 * Tokens with no annotations at all are reported as errors and skipped.
	 * Tokens annotated as `@Synthetic` that also carry lexical annotations
	 * are reported as errors and skipped.
	 */
	private fun buildResolvedTokens(tokenObjects: List<KSClassDeclaration>): List<ResolvedToken> =
		buildList {
			for (obj in tokenObjects) {
				val resolved = resolveToken(obj)
				if (resolved != null) add(resolved)
			}
		}

	private fun resolveToken(obj: KSClassDeclaration): ResolvedToken? {
		val operatorAnnotations: List<Operator> = obj.getAnnotationsByType()
		val keywordAnnotations: List<Keyword> = obj.getAnnotationsByType()
		val punctuationAnnotations: List<Punctuation> = obj.getAnnotationsByType()
		val syntheticAnnotations: List<Synthetic> = obj.getAnnotationsByType()
		val prefixAnnotations: List<Prefix> = obj.getAnnotationsByType()
		val suffixAnnotations: List<Suffix> = obj.getAnnotationsByType()
		val infixAnnotations: List<Infix> = obj.getAnnotationsByType()

		// Must have at least one kind annotation.
		val hasLexical = operatorAnnotations.isNotEmpty() ||
			keywordAnnotations.isNotEmpty() ||
			punctuationAnnotations.isNotEmpty()
		val hasSynthetic = syntheticAnnotations.isNotEmpty()

		if (!hasLexical && !hasSynthetic) {
			logger.error(
				"$obj has no @Operator, @Keyword, @Punctuation, or @Synthetic annotation",
				obj
			)
			return null
		}

		// Synthetic tokens must not carry lexical annotations.
		if (hasSynthetic) {
			if (hasLexical) {
				logger.error(
					"""
                    Conflict — $obj is annotated as @Synthetic
                    but also has @Operator/@Keyword/@Punctuation annotations.
                    Remove the other annotations.
                    """.trimIndent(),
					obj
				)
				return null
			}

			// Synthetic tokens can still have prefix/suffix/infix, though that would be unusual.
			// For now, just return the bare synthetic.
			return ResolvedToken(
				className = obj.toClassName(),
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
			className = obj.toClassName(),
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
	 * Ensures no two distinct token objects claim the same symbol string.
	 *
	 * Returns `true` if the grammar is valid, `false` if errors were reported.
	 * Duplicate visits of the *same* object (possible when it appears under
	 * multiple sealed parents) are silently deduplicated.
	 */
	private fun validateSymbolUniqueness(tokens: List<ResolvedToken>): Boolean {
		val symbolOwners = mutableMapOf<String, ResolvedToken>()
		var valid = true

		for (token in tokens) {
			val symbol = token.symbol ?: continue

			val existing = symbolOwners[symbol]
			if (existing != null && existing.className != token.className) {
				logger.error(
					"""
                    Conflict — same symbol '$symbol' registered twice:
                    * By ${existing.className}
                    * Now by ${token.className}
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