package net.landless_city.zigocracy.zon

import net.landless_city.zigocracy.grammar.annotations.*

/**
 * Operator precedence constants for expression parsing.
 *
 * Higher values bind tighter. ZON is a data format and does not define its own
 * infix operators; these values are reserved for upward compatibility with
 * full-Zig expression parsing.
 */
private object Precedence {
	const val BOOL_OR_EXPR = 10
	const val BOOL_AND_EXPR = 20
	const val COMPARE_OP = 30
	const val BITWISE_OP = 40
	const val BIT_SHIFT_OP = 50
	const val ADDITION_OP = 60
	const val MULTIPLY_OP = 70
}

@GrammarRoot
public enum class TokenKind {
	// region Keywords
	/** `true` literal. */
	@Keyword(symbol = "true")
	KeywordTrue,

	/** `false` literal. */
	@Keyword(symbol = "false")
	KeywordFalse,

	/** `null` literal. */
	@Keyword(symbol = "null")
	KeywordNull,

	/** `nan` literal. */
	@Keyword(symbol = "nan")
	KeywordNan,

	/** `inf` literal. */
	@Keyword(symbol = "inf")
	KeywordInf,
	// endregion

	// region Punctuation
	/** `.` — field access / decimal separator. */
	@Punctuation(symbol = ".")
	Dot,

	/** `{` — begin container literal. */
	@Punctuation(symbol = "{")
	LBrace,

	/** `}` — end container literal. */
	@Punctuation(symbol = "}")
	RBrace,

	/** `,` — element separator. */
	@Punctuation(symbol = ",")
	Comma,

	/** `=` — key-value separator. */
	@Punctuation(symbol = "=")
	Equals,

	/** `@` — quoted-identifier prefix. */
	@Punctuation(symbol = "@")
	At,
	// endregion

	// region Operators
	/** `-` — prefix negation for numeric literals. */
	@Operator(symbol = "-")
	@Prefix
	Minus,
	// endregion

	// region Synthetics
	/** User-defined identifier. */
	@Synthetic
	Identifier,

	/** `@"…"` — quoted identifier (raw string). */
	@Synthetic
	QuotedIdentifier,

	/** Integer literal (decimal, hex, octal, binary). */
	@Synthetic
	IntegerLiteral,

	/** Float literal. */
	@Synthetic
	FloatLiteral,

	/** String literal `"…"`. */
	@Synthetic
	StringLiteral,

	/** Multi-line string literal `\\…\\`. */
	@Synthetic
	MultilineStringLiteral,

	/** Character literal `'…'`. */
	@Synthetic
	CharLiteral,

	/** End-of-file sentinel. */
	@Synthetic
	EndOfFile,
	// endregion
}
