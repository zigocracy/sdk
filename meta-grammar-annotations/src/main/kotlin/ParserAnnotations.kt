/**
 * Annotations for marking tokens as expression operators with position and precedence information.
 *
 * These annotations are processed during compilation to generate operator lookup tables for
 * precedence-climbing expression parsing. The processor generates:
 * - An infix operator map with precedence and associativity information
 * - A prefix operator set
 * - A suffix operator set
 *
 * ## Operator Positions
 *
 * Tokens can participate in expression parsing in three positions:
 *
 * - **[Prefix]**: Before the operand (`-x`, `!condition`, `&value`)
 * - **[Infix]**: Between operands (`x + y`, `a == b`, `p or q`)
 * - **[Suffix]**: After the operand (`x++`, `value!!`, `ptr catch`)
 *
 * ## Combining Positions
 *
 * A single token can serve multiple roles by applying multiple annotations:
 *
 * ```kotlin
 * @Operator("-")
 * @Prefix // -x (negation)
 * @Infix(precedence = 50, associativity = Associativity.LEFT) // a - b (subtraction)
 * object Minus : OperatorToken
 * ```
 *
 * @see Prefix
 * @see Infix
 * @see Suffix
 * @see Associativity
 */
package net.landless_city.zigocracy.grammar.annotations

/**
 * Marks a token as usable in prefix position during expression parsing.
 *
 * Prefix operators appear before their operand: `-x`, `!condition`, `&value`.
 *
 * Example:
 * ```kotlin
 * @Operator("!")
 * @Prefix
 * object ExclamationMark : OperatorToken
 * ```
 *
 * @see Infix
 * @see Suffix
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FIELD)
public annotation class Prefix

/**
 * Marks a token as usable in suffix (postfix) position during expression parsing.
 *
 * Suffix operators appear after their operand: `x++`, `value!!`, `ptr.*`.
 *
 * Example:
 * ```kotlin
 * @Operator("!!")
 * @Suffix
 * object ExclamationMark2 : OperatorToken
 * ```
 *
 * @see Prefix
 * @see Infix
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FIELD)
public annotation class Suffix

/**
 * Marks a token as an infix (binary) operator with specified precedence and associativity.
 *
 * Infix operators appear between operands: `x + y`, `a == b`, `p or q`.
 *
 * **Precedence** determines binding strength (higher binds tighter):
 * ```
 * a + b * c  →  a + (b * c)  // `*` has higher precedence than `+`
 * ```
 *
 * **Associativity** determines grouping for equal precedence:
 * - [Associativity.LEFT]: `a - b - c` → `(a - b) - c`
 * - [Associativity.RIGHT]: `a = b = c` → `a = (b = c)`
 * - [Associativity.NONE]: `a < b < c` → parse error (cannot chain)
 *
 * Example:
 * ```kotlin
 * @Operator("+")
 * @Infix(precedence = 50, associativity = Associativity.LEFT)
 * object Plus : OperatorToken
 * ```
 *
 * @property precedence Binding strength (higher = tighter). Must be positive.
 * @property associativity Grouping direction for operators of equal precedence.
 *
 * @see Prefix
 * @see Suffix
 * @see Associativity
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FIELD)
public annotation class Infix(
	val precedence: Int,
	val associativity: Associativity,
)

/**
 * Associativity determines how operators of equal precedence group.
 *
 * - [LEFT]: Group left-to-right: `a - b - c` → `(a - b) - c`
 * - [RIGHT]: Group right-to-right: `a = b = c` → `a = (b = c)`
 * - [NONE]: Cannot be chained with any operator at the same precedence level
 *
 * ## Non-Associative Operators
 *
 * [NONE] prevents chaining of operators at the same precedence level. Once a
 * non-associative operator is used in an expression, no other operator at that
 * same precedence level can appear without parentheses, regardless of whether
 * it's the same operator or a different one.
 *
 * This is useful for comparison operators where chaining would be ambiguous or
 * error-prone. The restriction applies to the entire precedence level, not just
 * to the specific operator used.
 *
 * Operators at different precedence levels can still be combined normally, and
 * parentheses can be used to override the restriction by creating separate
 * sub-expressions.
 *
 * All operators at the same precedence level should have the same associativity.
 *
 * @see Infix
 */
public enum class Associativity {
	/** Left-associative: `(a - b) - c` */
	LEFT,

	/** Right-associative: `a - (b - c)` */
	RIGHT,

	/** Non-associative: cannot chain with any operator at the same precedence level */
	NONE,
}