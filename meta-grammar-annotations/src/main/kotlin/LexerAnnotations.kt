package net.landless_city.zigocracy.grammar.annotations

/**
 * Marks a token type as an operator with a fixed symbolic representation.
 *
 * Operators are symbolic tokens that represent operations or relationships in the language,
 * such as arithmetic (`+`, `*`), comparison (`==`, `<=`), logical (`&&`, `||`), or assignment
 * (`=`, `+=`) operators.
 *
 * The annotated class must be a singleton object (Kotlin `object`) representing a specific
 * operator token type. The [symbol] parameter defines the exact character sequence that will
 * be recognized by the lexer.
 *
 * ## Symbol Ordering
 *
 * When multiple operators share a common prefix (e.g., `>`, `>=`, `>>`), the lexer processor
 * automatically orders them by length (longest first) to ensure greedy matching. This means
 * `>>=` will be matched before `>>`, which will be matched before `>`.
 *
 * ## Example
 *
 * ```kotlin
 * @Operator(">=")
 * object RArrowEqual : TokenType()
 *
 * @Operator(">>")
 * object RArrow2 : TokenType()
 *
 * @Operator(">")
 * object RArrow : TokenType()
 * ```
 *
 * @property symbol The exact character sequence that represents this operator in source code.
 *                  Must be unique across all tokens in the grammar.
 *
 * @see Keyword
 * @see Punctuation
 * @see Synthetic
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FIELD)
public annotation class Operator(
	val symbol: String
)

/**
 * Marks a token type as a language keyword with a fixed textual representation.
 *
 * Keywords are reserved identifiers that have special meaning in the language and cannot
 * be used as user-defined names. Examples include control flow keywords (`if`, `while`),
 * declaration keywords (`fn`, `const`), and type keywords (`struct`, `enum`).
 *
 * The annotated class must be a singleton object (Kotlin `object`) representing a specific
 * keyword token type. The [symbol] parameter defines the exact text that will be recognized
 * by the lexer.
 *
 * ## Lexical Considerations
 *
 * Keywords are typically recognized as complete words. The lexer should ensure that keyword
 * matches are not followed by identifier-continuation characters (letters, digits, underscores)
 * to avoid matching `if` inside `ifElse` or `for` inside `format`.
 *
 * ## Example
 *
 * ```kotlin
 * @Keyword("if")
 * object KeywordIf : TokenType()
 *
 * @Keyword("while")
 * object KeywordWhile : TokenType()
 *
 * @Keyword("const")
 * object KeywordConst : TokenType()
 * ```
 *
 * @property symbol The exact keyword text that represents this token in source code.
 *                  Must be unique across all tokens in the grammar.
 *
 * @see Operator
 * @see Punctuation
 * @see Synthetic
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FIELD)
public annotation class Keyword(
	val symbol: String
)

/**
 * Marks a token type as punctuation with a fixed symbolic representation.
 *
 * Punctuation tokens are structural delimiters and separators that organize source code,
 * such as parentheses (`(`, `)`), braces (`{`, `}`), brackets (`[`, `]`), and separators
 * (`,`, `;`, `:`).
 *
 * The annotated class must be a singleton object (Kotlin `object`) representing a specific
 * punctuation token type. The [symbol] parameter defines the exact character sequence that
 * will be recognized by the lexer.
 *
 * ## Distinction from Operators
 *
 * While both punctuation and operators use symbolic characters, punctuation tokens primarily
 * serve structural or grouping purposes rather than representing operations. The distinction
 * is semantic: `,` is punctuation (separator), while `+` is an operator (addition).
 *
 * ## Example
 *
 * ```kotlin
 * @Punctuation("(")
 * object LeftParen : TokenType()
 *
 * @Punctuation(")")
 * object RightParen : TokenType()
 *
 * @Punctuation(";")
 * object Semicolon : TokenType()
 * ```
 *
 * @property symbol The exact character sequence that represents this punctuation in source code.
 *                  Must be unique across all tokens in the grammar.
 *
 * @see Operator
 * @see Keyword
 * @see Synthetic
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FIELD)
public annotation class Punctuation(
	val symbol: String
)

/**
 * Marks a token type as synthetic, meaning it has no direct lexical representation.
 *
 * Synthetic tokens are introduced by the parser or other compilation phases rather than
 * being directly recognized from source text. They represent abstract syntactic constructs
 * or intermediate parse states that don't correspond to actual characters in the input.
 *
 * Common uses for synthetic tokens include:
 * - End-of-file markers
 * - Error recovery tokens
 * - Implicit tokens inserted by the parser
 * - Abstract grouping or state tokens
 *
 * Tokens marked with `@Synthetic` are excluded from the generated lexer registry and cannot
 * be looked up by symbol. They must be created explicitly by the parser or compiler.
 *
 * ## Example
 *
 * ```kotlin
 * @Synthetic
 * object EndOfFile : TokenType()
 *
 * @Synthetic
 * object ErrorToken : TokenType()
 *
 * @Synthetic
 * object ImplicitSemicolon : TokenType()
 * ```
 *
 * ## Mutual Exclusivity
 *
 * A token type cannot be both synthetic and have a lexical representation. Do not combine
 * `@Synthetic` with `@Operator`, `@Keyword`, or `@Punctuation` annotations.
 *
 * @see Operator
 * @see Keyword
 * @see Punctuation
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FIELD)
public annotation class Synthetic