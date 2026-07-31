package net.landless_city.zigocracy.zig.scanner.util

import net.landless_city.zigocracy.zig.text.CodeUnits
import net.landless_city.zigocracy.zig.text.TextReader

internal enum class NumberBase(val radix: Int) {
	Binary(radix = 2) {
		override fun isValidDigit(c: Char): Boolean = c.isZigBinaryDigit()
		override fun isExponentMarker(c: Char): Boolean = false
		override fun supportsFloat(): Boolean = false
	},
	Octal(radix = 8) {
		override fun isValidDigit(c: Char): Boolean = c.isZigOctalDigit()
		override fun isExponentMarker(c: Char): Boolean = false
		override fun supportsFloat(): Boolean = false
	},
	Decimal(radix = 10) {
		override fun isValidDigit(c: Char): Boolean = c.isZigDecimalDigit()
		override fun isExponentMarker(c: Char): Boolean = c == 'e' || c == 'E'
		override fun supportsFloat(): Boolean = true
	},
	Hexadecimal(radix = 16) {
		override fun isValidDigit(c: Char): Boolean = c.isZigHexDigit()
		override fun isExponentMarker(c: Char): Boolean = c == 'p' || c == 'P'
		override fun supportsFloat(): Boolean = true
	};

	abstract fun isValidDigit(c: Char): Boolean
	abstract fun isExponentMarker(c: Char): Boolean

	/**
	 * Flags whether the radix permits fractional point notation.
	 *
	 * Examples:
	 * - "1.5"    -> Allowed for Decimal.
	 * - "0x1.5p2" -> Allowed for Hexadecimal.
	 * - "0b1.1"   -> Forbidden for Binary;
	 */
	abstract fun supportsFloat(): Boolean

	fun isValidCharacterInLiteral(c: Char): Boolean {
		return isValidDigit(c) || isExponentMarker(c) || c == '_'
	}

	/**
	 * Evaluates and safely skips a unary sign operator belonging to a trailing exponent block.
	 *
	 * Examples:
	 * - "1e+10" -> Skips '+' and returns 1, meaning the total stride step is 2 (marker + sign).
	 * - "1e-5"  -> Skips '-' and returns 1, meaning the total stride step is 2 (marker + sign).
	 * - "1e4"   -> No sign detected; returns 0, preserving execution stride.
	 */
	fun consumeExponentSign(reader: TextReader, currentWidth: CodeUnits): CodeUnits {
		val next = reader.peekChar(currentWidth + 1)
		return if (next == '+' || next == '-') 1 else 0
	}
}