package net.landless_city.zigocracy.grammar.annotations

/**
 * Marks the enum class that serves as the root of the token hierarchy.
 *
 * Exactly one enum class in the compilation unit should carry this annotation.
 * The annotation processor will collect all annotated enum entries of this type
 * and extract the grammar from their `@Operator`, `@Keyword`, `@Punctuation`,
 * `@Synthetic`, `@Prefix`, `@Suffix`, and `@Infix` annotations.
 *
 * ## Example
 *
 * ```kotlin
 * @GrammarRoot
 * enum class TokenKind {
 *     @Keyword("if")   KeywordIf,
 *     @Keyword("else") KeywordElse,
 *     @Operator("+") @Infix(precedence = 60, associativity = Associativity.LEFT) Plus,
 *     @Punctuation(";") Semicolon,
 *     @Synthetic Identifier,
 *     EndOfFile,
 * }
 * ```
 *
 * @see Keyword
 * @see Operator
 * @see Punctuation
 * @see Synthetic
 * @see Prefix
 * @see Infix
 * @see Suffix
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
public annotation class GrammarRoot
