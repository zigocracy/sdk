package net.landless_city.zigocracy.zon

/**
 * A single lexical token produced by [ZonLexer].
 *
 * @property kind  The token kind (keyword, punctuation, operator, synthetic).
 * @property text  The raw source text that matched.
 * @property offset Byte offset from the start of input.
 */
public data class Token(
	val kind: TokenKind,
	val text: String,
	val offset: Int,
)
