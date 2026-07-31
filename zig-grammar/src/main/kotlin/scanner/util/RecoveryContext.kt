package net.landless_city.zigocracy.zig.scanner.util

import net.landless_city.zigocracy.zig.text.CodeUnits
import net.landless_city.zigocracy.zig.text.TextReader

/**
 * Defines context-specific recovery behaviors and token boundary invariants
 * for panic-mode error recovery during lexical analysis.
 */
internal enum class RecoveryContext {
	StringLiteral {
		override val terminalChar: Char get() = '"'

		override fun isSynchronizing(c: Char): Boolean =
			c == '"' || c.isZigVerticalWhitespace()

		override fun isInteriorWhitespace(reader: TextReader, base: CodeUnits, currentWidth: CodeUnits): Boolean {
			var lookahead = currentWidth
			while (reader.peekChar(base + lookahead)?.isZigHorizontalWhitespace() == true) {
				lookahead++
			}
			val next = reader.peekChar(base + lookahead)

			// Examples of internal string space routing:
			// - `"hello world"` -> Followed by 'w'; evaluates to true (valid literal payload).
			// - `"hello   \n`  -> Followed by newline; evaluates to false (trailing garbage spaces).
			return next != null && !next.isZigVerticalWhitespace()
		}
	},

	CharLiteral {
		override val terminalChar: Char get() = '\''

		override fun isSynchronizing(c: Char): Boolean =
			c == '\'' || c.isZigVerticalWhitespace()

		override fun isInteriorWhitespace(reader: TextReader, base: CodeUnits, currentWidth: CodeUnits): Boolean {
			var lookahead = currentWidth
			while (reader.peekChar(base + lookahead)?.isZigHorizontalWhitespace() == true) {
				lookahead++
			}

			// Examples of internal char space routing:
			// - `' '`      -> Followed immediately by single quote; evaluates to true.
			// - `'a   \n`  -> Followed by newline/EOF without closing quote; evaluates to false.
			return reader.peekChar(base + lookahead) == terminalChar
		}
	};

	/**
	 * The literal character that successfully terminates and closes this syntactic context.
	 */
	abstract val terminalChar: Char

	/**
	 * Determines if a character represents a hard syntax barrier where the current
	 * panic-mode recovery loop must halt to prevent cascading parser failures.
	 */
	abstract fun isSynchronizing(c: Char): Boolean

	/**
	 * Determines whether a sequence of horizontal whitespace characters should be absorbed
	 * as part of a malformed literal payload or preserved to maintain top-level trivia tokenization.
	 *
	 * @param reader The text stream viewer.
	 * @param base The token-relative position where the error sequence started.
	 * @param currentWidth The length of the malformed sequence scanned so far.
	 * @return True if the spaces are safely bound inside the literal and can be consumed.
	 */
	abstract fun isInteriorWhitespace(reader: TextReader, base: CodeUnits, currentWidth: CodeUnits): Boolean
}

internal fun Char.isSynchronizingFor(context: RecoveryContext): Boolean = context.isSynchronizing(this)