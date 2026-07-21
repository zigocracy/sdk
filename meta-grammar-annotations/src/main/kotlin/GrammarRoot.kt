package net.landless_city.zigocracy.grammar.annotations

/**
 * Marks the sealed class or sealed interface that serves as the root of the token hierarchy.
 *
 * Exactly one sealed class or sealed interface in the compilation unit should carry this
 * annotation. The annotation processor will collect all concrete `object` subclasses of this
 * type and extract the grammar from their `@Operator`, `@Keyword`, `@Punctuation`,
 * `@Synthetic`, `@Prefix`, `@Suffix`, and `@Infix` annotations.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
public annotation class GrammarRoot