package com.zigocracy.sdk.zon

import language.syntax.GeneratedLexerRegistry

/**
 * A simple regex-based lexer for ZON.
 *
 * Static tokens (keywords, punctuation, operators) come from the generated
 * [GeneratedLexerRegistry.stringToToken] map. Dynamic/synthetic tokens
 * (identifiers, literals) are matched via regex patterns.
 *
 * Whitespace and line comments are skipped. Keywords require word boundaries.
 */
public class ZonLexer(private val input: String) {

	private var pos: Int = 0

	private val matchers: List<Matcher> by lazy { buildMatchers() }

	/** Tokenize the entire input. Returns either the token list or a diagnostic. */
	public fun tokenize(): LexResult {
		return try {
			val tokens = mutableListOf<Token>()
			pos = 0

			while (pos < input.length) {
				if (skipWhitespace() || skipComment()) continue

				val matched = matchToken()
					?: throw LexerException(
						diagnosticAt(
							source = input,
							offset = pos,
							message = "Unexpected character '${input[pos]}'",
						)
					)
				tokens.add(matched)
				pos += matched.text.length
			}

			tokens.add(Token(TokenKind.EndOfFile, "", pos))
			LexResult.Success(tokens)
		} catch (e: LexerException) {
			LexResult.Error(e.diagnostic)
		}
	}

	// region Skip helpers

	private fun skipWhitespace(): Boolean {
		val m = WS.matchAt(input, pos) ?: return false
		pos = m.range.last + 1
		return true
	}

	private fun skipComment(): Boolean {
		val m = COMMENT.matchAt(input, pos) ?: return false
		pos = m.range.last + 1
		return true
	}

	// endregion
	// region Matchers — built from generated registry + synthetic patterns

	private data class Matcher(val kind: TokenKind, val pattern: Regex)

	private fun buildMatchers(): List<Matcher> {
		val kwSet = setOf(
			TokenKind.KeywordTrue, TokenKind.KeywordFalse, TokenKind.KeywordNull,
			TokenKind.KeywordNan, TokenKind.KeywordInf,
		)

		return buildList {
			// 1. Keywords from the registry — word-boundary guard.
			for ((symbol, kind) in GeneratedLexerRegistry.stringToToken
				.entries
				.filter { it.value in kwSet }
				.sortedByDescending { it.key.length }
			) {
				add(Matcher(kind, wholeWord(symbol)))
			}

			// 2. Synthetic patterns with distinctive leading characters
			add(Matcher(TokenKind.QuotedIdentifier, QUOTED_ID))
			add(Matcher(TokenKind.MultilineStringLiteral, STRING_MULTI))
			add(Matcher(TokenKind.StringLiteral, STRING_SINGLE))
			add(Matcher(TokenKind.CharLiteral, CHAR_LIT))
			add(Matcher(TokenKind.FloatLiteral, FLOAT_LIT))
			add(Matcher(TokenKind.IntegerLiteral, INT_LIT))
			add(Matcher(TokenKind.Identifier, PLAIN_ID))

			// 3. Static punctuation & operators from the registry.
			for ((symbol, kind) in GeneratedLexerRegistry.stringToToken
				.entries
				.filter { it.value !in kwSet }
				.sortedByDescending { it.key.length }
			) {
				add(Matcher(kind, Regex.fromLiteral(symbol)))
			}
		}
	}

	private fun matchToken(): Token? {
		for (m in matchers) {
			val mt = m.pattern.find(input, pos)
			if (mt != null && mt.range.first == pos) {
				return Token(m.kind, mt.value, pos)
			}
		}
		return null
	}

	// endregion
	// region Pattern definitions

	private fun wholeWord(w: String): Regex =
		Regex("""\b${Regex.escape(w)}\b""")

	private companion object {
		private val WS = Regex("""\s+""")
		private val COMMENT = Regex("""//[^\n]*""")

		// region Identifiers

		private val PLAIN_ID = Regex("""[a-zA-Z_][a-zA-Z0-9_]*""")
		private val QUOTED_ID = Regex("@\"(?:[^\"\\\\]|\\\\.)*\"")

		// endregion
		// region Strings & chars

		private val ESCAPE = """\\[nr\\t'"]""".toRegex()
		private val ESC_HEX = """\\x[0-9a-fA-F]{2}""".toRegex()
		private val ESC_UNI = """\\u\{[0-9a-fA-F]+\}""".toRegex()
		private val STR_CHAR = """[^"\\]""".toRegex()
		private val STRING_SINGLE = Regex(
			"\"" + "(?:${ESCAPE.pattern}|${ESC_HEX.pattern}|${ESC_UNI.pattern}|${STR_CHAR.pattern})*" + "\""
		)
		private val STRING_MULTI = Regex("""\\\\[^\n]*""")
		private val CHAR_UNESC = """[^'\\]""".toRegex()
		private val CHAR_LIT = Regex(
			"'" + "(?:${ESCAPE.pattern}|${ESC_HEX.pattern}|${ESC_UNI.pattern}|${CHAR_UNESC.pattern})" + "'"
		)

		// endregion
		// region Number parts

		private const val HEX_START = """[0-9a-fA-F][0-9a-fA-F_]*"""
		private const val HEX_FRAC = """[0-9a-fA-F_]+"""
		private const val OCT_START = """[0-7][0-7_]*"""
		private const val BIN_START = """[01][01_]*"""
		private const val DEC_START = """[0-9][0-9_]*"""
		private const val DEC_FRAC = """[0-9_]+"""

		private const val P_EXP_PATTERN = """[pP][-+]?""" + DEC_FRAC
		private const val E_EXP_PATTERN = """[eE][-+]?""" + DEC_FRAC

		// region Integers

		private val HEX_INT = Regex("""0[xX]""" + HEX_START)
		private val OCT_INT = Regex("""0[oO]""" + OCT_START)
		private val BIN_INT = Regex("""0[bB]""" + BIN_START)
		private val DEC_INT = Regex(DEC_START)
		private val INT_LIT = Regex(
			"(?:" + HEX_INT.pattern + "|" + OCT_INT.pattern + "|" + BIN_INT.pattern + "|" + DEC_INT.pattern + ")"
		)

		// endregion
		// region Floats

		private val HEX_FLOAT_FRAC = Regex("""0[xX]""" + HEX_START + """\.""" + HEX_FRAC + "(?:" + P_EXP_PATTERN + ")?")
		private val HEX_FLOAT_EXP = Regex("""0[xX]""" + HEX_START + "(?:" + P_EXP_PATTERN + ")")
		private val DEC_FLOAT_FRAC = Regex(DEC_START + """\.""" + DEC_FRAC + "(?:" + E_EXP_PATTERN + ")?")
		private val DEC_FLOAT_EXP = Regex(DEC_START + "(?:" + E_EXP_PATTERN + ")")
		private val FLOAT_LIT = Regex(
			"(?:" + HEX_FLOAT_FRAC.pattern + "|" + HEX_FLOAT_EXP.pattern + "|" + DEC_FLOAT_FRAC.pattern + "|" + DEC_FLOAT_EXP.pattern + ")"
		)

		// endregion
		// endregion
	}

	// endregion
}

/** Internal exception that carries a structured [Diagnostic] through the lexer. */
internal class LexerException(val diagnostic: Diagnostic) : Exception()
