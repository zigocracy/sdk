package net.landless_city.zigocracy.zon

import java.math.BigDecimal
import java.math.BigInteger

/**
 * ZON AST — the parse-tree representation of a ZON value.
 */
public sealed interface ZonAstNode {

	// region Containers

	/** `. { }` — empty struct. */
	data object EmptyStruct : ZonAstNode

	/** `. { keyed_fields }` — struct with named field entries. */
	data class KeyedStruct(
		val fields: List<FieldInit>,
	) : ZonAstNode

	/** `. { positional_values }` — struct with positional entries. */
	data class ArrayStruct(
		val values: List<ZonAstNode>,
	) : ZonAstNode

	/** `.identifier = value` — a single field binding inside a struct. */
	data class FieldInit(
		val name: Identifier,
		val value: ZonAstNode,
	) : ZonAstNode

	/** `.identifier` — enum variant. */
	data class EnumLiteral(
		val name: Identifier,
	) : ZonAstNode

	// endregion
	// region Identifiers

	sealed interface Identifier : ZonAstNode {
		data class Plain(val name: String) : Identifier
		data class Quoted(val raw: String) : Identifier
	}

	// endregion
	// region Leaf values

	/** `true` */
	data object TrueVal : ZonAstNode

	/** `false` */
	data object FalseVal : ZonAstNode

	/** `null` */
	data object NullVal : ZonAstNode

	/** `nan` */
	data object NanVal : ZonAstNode

	/** `inf` or `-inf` */
	data class InfVal(val negated: Boolean = false) : ZonAstNode

	/** Integer literal — parsed value (supports decimal, hex `0x`, octal `0o`, binary `0b`). */
	data class IntVal(val value: BigInteger) : ZonAstNode

	/** Float literal — parsed value (supports decimal, scientific `e`, hex `0xp`). */
	data class FloatVal(val value: BigDecimal) : ZonAstNode

	/** String literal `"…"`. */
	data class SingleString(val value: String) : ZonAstNode

	/** Multi-line string `\\…\\`. */
	data class MultilineString(val lines: List<String>) : ZonAstNode

	/** Character literal `'…'`. */
	data class CharVal(val codepoint: Int) : ZonAstNode

	// endregion
}
