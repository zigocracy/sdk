package net.landless_city.zigocracy.zon

import net.landless_city.zigocracy.zon.ZonAstNode.*
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Recursive-descent parser for ZON.
 */
public class ZonParser(
	private val tokens: List<Token>,
	private val source: String,
) {

	private var pos: Int = 0

	/** Parse the full token stream into either an AST node or a structured diagnostic. */
	public fun parse(): ParseResult {
		return try {
			pos = 0
			val node = parseExpr()
			if (!atEnd()) {
				val tok = peek()
				throw ParserException(
					diagnosticAt(
						source = source,
						offset = tok.offset,
						message = "Unexpected token '${tok.text}'",
					)
				)
			}
			ParseResult.Success(node)
		} catch (e: ParserException) {
			ParseResult.Error(e.diagnostic)
		}
	}

	// region expr dispatch

	private fun parseExpr(): ZonAstNode = when (peek().kind) {
		TokenKind.Dot -> {
			val saved = pos
			advance()
			when (peek().kind) {
				TokenKind.LBrace -> {
					pos = saved; parseStructInit()
				}

				else -> {
					pos = saved; parseEnumLiteral()
				}
			}
		}

		TokenKind.StringLiteral -> parseString()
		TokenKind.MultilineStringLiteral -> parseMultilineString()
		TokenKind.FloatLiteral,
		TokenKind.IntegerLiteral,
		TokenKind.KeywordNan,
		TokenKind.KeywordInf,
		TokenKind.Minus -> parseNumber()

		TokenKind.KeywordTrue,
		TokenKind.KeywordFalse -> parseBool()

		TokenKind.KeywordNull -> parseNull()
		TokenKind.CharLiteral -> parseCharLiteral()

		else -> {
			val tok = peek()
			throw ParserException(
				diagnosticAt(
					source = source,
					offset = tok.offset,
					message = "Expected expression, got '${tok.text}'",
				)
			)
		}
	}

	// endregion
	// region struct_init

	private fun parseStructInit(): ZonAstNode {
		consume(TokenKind.Dot, "'.'")
		consume(TokenKind.LBrace, "'{'")

		if (tryConsume(TokenKind.RBrace)) {
			return EmptyStruct
		}

		val body = parseStructBody()
		consume(TokenKind.RBrace, "'}'")
		return body
	}

	// endregion
	// region struct_body

	private fun parseStructBody(): ZonAstNode =
		if (lookaheadIsKeyed()) {
			KeyedStruct(parseCommaSeparated { parseFieldInit() })
		} else {
			ArrayStruct(parseCommaSeparated { parseExpr() })
		}

	private fun lookaheadIsKeyed(): Boolean {
		if (pos + 2 >= tokens.size) return false
		return tokens[pos].kind == TokenKind.Dot &&
			tokens[pos + 1].kind in listOf(
			TokenKind.Identifier, TokenKind.QuotedIdentifier
		) &&
			tokens[pos + 2].kind == TokenKind.Equals
	}

	// endregion
	// region field_init

	private fun parseFieldInit(): FieldInit {
		consume(TokenKind.Dot, "'.'")
		val name = parseIdentifier()
		consume(TokenKind.Equals, "'='")
		val value = parseExpr()
		return FieldInit(name, value)
	}

	// endregion
	// region enum_literal

	private fun parseEnumLiteral(): EnumLiteral {
		consume(TokenKind.Dot, "'.'")
		val name = parseIdentifier()
		return EnumLiteral(name)
	}

	// endregion
	// region identifier

	private fun parseIdentifier(): Identifier = when (peek().kind) {
		TokenKind.Identifier -> {
			advance()
			Identifier.Plain(tokens[pos - 1].text)
		}

		TokenKind.QuotedIdentifier -> {
			advance()
			val raw = tokens[pos - 1].text
			val inner = raw.removeSurrounding("@\"", "\"")
			Identifier.Quoted(inner)
		}

		else -> {
			val tok = peek()
			throw ParserException(
				diagnosticAt(
					source = source,
					offset = tok.offset,
					message = "Expected identifier, got '${tok.text}'",
				)
			)
		}
	}

	// endregion
	// region string

	private fun parseString(): SingleString {
		val tok = consume(TokenKind.StringLiteral, "string literal")
		val inner = tok.text.removeSurrounding("\"")
		return SingleString(unescape(inner))
	}

	private fun parseMultilineString(): MultilineString {
		val lines = mutableListOf<String>()
		do {
			val tok = advance()
			val line = tok.text.removePrefix("\\\\")
			lines.add(line)
		} while (peek().kind == TokenKind.MultilineStringLiteral)
		return MultilineString(lines)
	}

	// endregion
	// region bool

	private fun parseBool(): ZonAstNode = when (advance().kind) {
		TokenKind.KeywordTrue -> TrueVal
		TokenKind.KeywordFalse -> FalseVal
		else -> error("Unreachable")
	}

	// endregion
	// region null_val

	private fun parseNull(): NullVal {
		consume(TokenKind.KeywordNull, "'null'")
		return NullVal
	}

	// endregion
	// region char_literal

	private fun parseCharLiteral(): CharVal {
		val tok = consume(TokenKind.CharLiteral, "char literal")
		val inner = tok.text.removeSurrounding("'")
		val codepoint = unescape(inner).firstOrNull()?.code ?: 0
		return CharVal(codepoint)
	}

	// endregion
	// region Parsing helpers

	/** Parse an integer literal (decimal, hex `0x`, octal `0o`, binary `0b`) into [BigInteger]. */
	private fun parseBigInt(raw: String): BigInteger {
		val cleaned = raw.replace("_", "")
		val negative = cleaned.startsWith('-')
		val body = if (negative) cleaned.drop(1) else cleaned
		val (digits, radix) = when {
			body.startsWith("0x", ignoreCase = true) -> body.drop(2) to 16
			body.startsWith("0o", ignoreCase = true) -> body.drop(2) to 8
			body.startsWith("0b", ignoreCase = true) -> body.drop(2) to 2
			else -> body to 10
		}
		val value = BigInteger(digits, radix)
		return if (negative) value.negate() else value
	}

	/** Parse a float literal into [BigDecimal] (hex floats go through Double or manual parse). */
	private fun parseBigDecimal(raw: String): BigDecimal {
		val cleaned = raw.replace("_", "")
		return when {
			cleaned.startsWith("-0x", ignoreCase = true) || cleaned.startsWith("0x", ignoreCase = true) -> {
				val hasExponent = cleaned.contains("p", ignoreCase = true)
				if (hasExponent) {
					// Java handles hex-with-exponent fine: "0x1.fP3" → 15.5
					BigDecimal.valueOf(cleaned.toDouble())
				} else {
					// Manual parse for hex float WITHOUT p-exponent, e.g. "0x103.70"
					parseHexFloat(cleaned)
				}
			}

			else -> BigDecimal(cleaned)
		}
	}

	/** Parse `0xAAA.BBB` where no `p`/`P` exponent is present. */
	private fun parseHexFloat(raw: String): BigDecimal {
		val negative = raw.startsWith('-')
		val body = if (negative) raw.drop(1) else raw
		val dot = body.indexOf('.')
		val intPart = body.substring(2, if (dot < 0) body.length else dot)
		val fracPart = if (dot < 0) "" else body.substring(dot + 1)

		val intValue = if (intPart.isNotEmpty())
			BigInteger(intPart, 16).toDouble()
		else 0.0

		val fracValue = if (fracPart.isNotEmpty()) {
			val fracNum = BigInteger(fracPart, 16).toDouble()
			val divisor = BigInteger.valueOf(16).pow(fracPart.length).toDouble()
			fracNum / divisor
		} else 0.0

		val result = BigDecimal.valueOf(intValue + fracValue)
		return if (negative) result.negate() else result
	}

	// endregion
	// region number

	private fun parseNumber(): ZonAstNode {
		val negated = tryConsume(TokenKind.Minus)

		return when (peek().kind) {
			TokenKind.KeywordNan -> {
				advance()
				if (negated) throw ParserException(
					diagnosticAt(source, tokens[pos - 1].offset, "'-nan' is not valid ZON")
				)
				NanVal
			}

			TokenKind.KeywordInf -> {
				advance(); InfVal(negated)
			}

			TokenKind.FloatLiteral -> {
				val tok = advance()
				val raw = if (negated) "-${tok.text}" else tok.text
				FloatVal(parseBigDecimal(raw))
			}

			TokenKind.IntegerLiteral -> {
				val tok = advance()
				val raw = if (negated) "-${tok.text}" else tok.text
				IntVal(parseBigInt(raw))
			}

			else -> {
				val tok = peek()
				throw ParserException(
					diagnosticAt(source, tok.offset, "Expected number literal, got '${tok.text}'")
				)
			}
		}
	}

	// endregion
	// region Low-level token helpers

	private fun peek(): Token = tokens[pos]
	private fun advance(): Token = tokens[pos++]
	private fun atEnd(): Boolean = pos >= tokens.size || tokens[pos].kind == TokenKind.EndOfFile

	private fun consume(expected: TokenKind, desc: String): Token {
		if (peek().kind != expected) {
			val tok = peek()
			throw ParserException(
				diagnosticAt(
					source = source,
					offset = tok.offset,
					message = "Expected $desc, got '${tok.text}'",
				)
			)
		}
		return advance()
	}

	private fun tryConsume(kind: TokenKind): Boolean {
		if (peek().kind == kind) {
			advance(); return true
		}
		return false
	}

	private fun <T> parseCommaSeparated(parseItem: () -> T): List<T> {
		val items = mutableListOf(parseItem())
		while (tryConsume(TokenKind.Comma)) {
			if (peek().kind == TokenKind.RBrace) break
			items.add(parseItem())
		}
		return items
	}

	// endregion
	// region String unescaping

	private fun unescape(s: String): String {
		val sb = StringBuilder(s.length)
		var i = 0
		while (i < s.length) {
			if (s[i] == '\\' && i + 1 < s.length) {
				when (s[i + 1]) {
					'x' -> {
						// \xNN — exactly 2 hex digits
						if (i + 3 < s.length) {
							val hex = s.substring(i + 2, i + 4)
							sb.append(hex.toInt(16).toChar())
							i += 4
						} else {
							sb.append(s[i]); i++
						}
					}

					'u' -> {
						// \u{NNNN} — one or more hex digits in braces
						val close = s.indexOf('}', i + 2)
						if (close > i + 3) {
							val hex = s.substring(i + 3, close)
							sb.append(hex.toInt(16).toChar())
							i = close + 1
						} else {
							sb.append(s[i]); i++
						}
					}

					else -> {
						sb.append(unescapeSimple(s.substring(i, i + 2)))
						i += 2
					}
				}
			} else {
				sb.append(s[i])
				i++
			}
		}
		return sb.toString()
	}

	private fun unescapeSimple(seq: String): String = when (seq) {
		"\\n" -> "\n"
		"\\r" -> "\r"
		"\\t" -> "\t"
		"\\\\" -> "\\"
		"\\'" -> "'"
		"\\\"" -> "\""
		else -> seq
	}

	// endregion
}

/** Internal exception that carries a structured [Diagnostic] through the parser. */
internal class ParserException(val diagnostic: Diagnostic) : Exception()
