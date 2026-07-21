package net.landless_city.zigocracy.processor

import com.squareup.kotlinpoet.ClassName
import net.landless_city.zigocracy.grammar.annotations.Associativity

/**
 * The fully resolved grammar of the language, extracted from annotations at compile time.
 *
 * This is the intermediate representation that sits between the **Resolver** (which reads
 * KSP annotations and validates the grammar) and the **Emitters** (which generate Kotlin
 * source files, documentation, or other build artifacts).
 *
 * ## Design intent
 *
 * By concentrating all annotation-derived facts into this single model, we ensure:
 *
 * - **Emitters never touch KSP.** They receive pure Kotlin data classes and can be
 *   unit-tested without a compiler environment.
 * - **Validation happens once.** The Resolver checks for duplicate symbols, conflicting
 *   annotations, and missing metadata before constructing the model. Emitters can trust
 *   that every invariant holds.
 * - **New emitters are trivial to add.** A Markdown docs emitter, a TextMate grammar
 *   emitter, or an LSP semantic-tokens emitter each just implement [GrammarEmitter]
 *   and consume this model.
 *
 * @property rootType The enum root of the token hierarchy (e.g. `language.syntax.TokenKind`).
 * @property tokens Every token entry discovered in the enum, with all of its
 *     resolved metadata (symbol, kind, operator position, precedence, etc.).
 *
 * @see GrammarResolver
 * @see GrammarEmitter
 */
public data class ResolvedGrammar(
	val rootType: ClassName,
	val tokens: List<ResolvedToken>
) {
	// ── Derived views (computed once, cached) ───────────────────────────

	/** All tokens that carry a lexical symbol (keywords, operators, punctuation). */
	val staticTokens: List<ResolvedToken> by lazy {
		tokens.filter { it.kind != null }
	}

	/** Tokens annotated as keywords. */
	val keywords: List<ResolvedToken> by lazy {
		tokens.filter { it.kind == TokenKind.KEYWORD }
	}

	/** Tokens annotated as operators. */
	val operators: List<ResolvedToken> by lazy {
		tokens.filter { it.kind == TokenKind.OPERATOR }
	}

	/** Tokens annotated as punctuation. */
	val punctuation: List<ResolvedToken> by lazy {
		tokens.filter { it.kind == TokenKind.PUNCTUATION }
	}

	/** Tokens that participate in infix expressions, sorted by precedence then name. */
	val infixOperators: List<ResolvedToken> by lazy {
		tokens.filter { it.infix != null }
			.sortedWith(compareBy({ it.infix!!.precedence }, { it.entryName }))
	}

	/** Tokens usable in prefix position. */
	val prefixOperators: List<ResolvedToken> by lazy {
		tokens.filter { it.isPrefix }
	}

	/** Tokens usable in suffix position. */
	val suffixOperators: List<ResolvedToken> by lazy {
		tokens.filter { it.isSuffix }
	}
}

/**
 * Complete metadata for a single token entry, resolved from annotations.
 *
 * A token may be `@Synthetic` (in which case [kind] and [symbol] are both `null`)
 * or it may carry exactly one of `@Keyword`, `@Operator`, or `@Punctuation`
 * (in which case [kind] and [symbol] are both non-null).
 *
 * Operator-position annotations (`@Prefix`, `@Infix`, `@Suffix`) are orthogonal
 * and may be combined freely.
 *
 * @property className The fully-qualified [ClassName] of the enum class (e.g. `TokenKind`).
 * @property entryName The name of the enum entry (e.g. `"KeywordIf"`, `"Plus"`).
 * @property symbol The character sequence this token matches, or `null` for synthetics.
 * @property kind The lexical category, or `null` for synthetics.
 * @property isPrefix Whether this token is annotated with `@Prefix`.
 * @property isSuffix Whether this token is annotated with `@Suffix`.
 * @property infix The infix metadata (precedence + associativity), or `null`.
 * @property isSynthetic Whether this token is annotated with `@Synthetic`.
 */
public data class ResolvedToken(
	val className: ClassName,
	val entryName: String,
	val symbol: String?,
	val kind: TokenKind?,
	val isPrefix: Boolean,
	val isSuffix: Boolean,
	val infix: ResolvedInfix?,
	val isSynthetic: Boolean
)

/**
 * Resolved infix-operator metadata extracted from an `@Infix` annotation.
 *
 * @property precedence Binding strength (higher = tighter).
 * @property associativity Grouping direction for operators at equal precedence.
 */
public data class ResolvedInfix(
	val precedence: Int,
	val associativity: Associativity
)

/**
 * The lexical category of a static token.
 *
 * This distinction matters for code generation: keywords require word-boundary
 * validation during scanning, whereas operators and punctuation are matched
 * purely by character sequence.
 */
public enum class TokenKind {
	KEYWORD,
	OPERATOR,
	PUNCTUATION;

	/** Whether scanning this kind of token requires a word-boundary check. */
	public val requiresBoundaryCheck: Boolean get() = this == KEYWORD
}
