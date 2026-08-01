package net.landless_city.zigocracy.zig.scanner.util

import net.landless_city.zigocracy.zig.scanner.TextReader
import net.landless_city.zigocracy.zig.shared.CodeUnits

object ScannerUtils {
	fun isHorizontalWhitespace(c: Char): Boolean = when (c) {
		' ', '\t' -> true
		else -> false
	}

	fun isVerticalWhitespace(c: Char): Boolean = when (c) {
		'\n', '\r' -> true
		else -> false
	}

	fun isSimpleEscape(c: Char): Boolean = when (c) {
		'n', 'r', 't', '\\', '\'', '"' -> true
		else -> false
	}

	//

	fun getBinaryValue(c: Char): Int? = when (c) {
		'0' -> 0
		'1' -> 1
		else -> null
	}

	fun isBinaryDigit(c: Char): Boolean = getBinaryValue(c) != null

	fun getOctalValue(c: Char): Int? = when (c) {
		in '0'..'7' -> c - '0'
		else -> null
	}

	fun isOctalDigit(c: Char): Boolean = getOctalValue(c) != null

	fun getDecimalValue(c: Char): Int? = when (c) {
		in '0'..'9' -> c - '0'
		else -> null
	}

	fun isDecimalDigit(c: Char): Boolean = getDecimalValue(c) != null

	fun getHexValue(c: Char): Int? = when (c) {
		in '0'..'9' -> c - '0'
		in 'a'..'f' -> c - 'a' + 10
		in 'A'..'F' -> c - 'A' + 10
		else -> null
	}

	fun isHexDigit(c: Char): Boolean = getHexValue(c) != null

	fun getVerticalWhitespaceWidth(c: Char, reader: TextReader, offset: CodeUnits): CodeUnits = when (c) {
		'\r' -> if (reader.peekChar(offset + 1) == '\n') 2 else 1
		else -> 1
	}

	fun isBuiltinIdentifierStart(c: Char): Boolean = when (c) {
		in 'a'..'z', in 'A'..'Z', '_' -> true
		else -> false
	}

	fun isBuiltinIdentifierPart(c: Char): Boolean = when (c) {
		in 'a'..'z', in 'A'..'Z', in '0'..'9', '_' -> true
		else -> false
	}

	fun isUserIdentifierStart(c: Int): Boolean =
		c == '_'.code || Character.isUnicodeIdentifierStart(c)


	fun isUserIdentifierPart(c: Int): Boolean =
		Character.isUnicodeIdentifierPart(c)

	fun isNumberBodyPart(c: Char): Boolean = when (c) {
		in '0'..'9', in 'a'..'z', in 'A'..'Z', '_' -> true
		else -> false
	}

	fun isValidFloatFractionStart(c: Char): Boolean = when (c) {
		in '0'..'9', in 'a'..'z', in 'A'..'Z', '_' -> true
		else -> false
	}
}

internal fun Char.isZigHorizontalWhitespace(): Boolean = ScannerUtils.isHorizontalWhitespace(this)
internal fun Char.isZigVerticalWhitespace(): Boolean = ScannerUtils.isVerticalWhitespace(this)
internal fun Char.isZigSimpleEscape(): Boolean = ScannerUtils.isSimpleEscape(this)

internal fun Char.isZigBinaryDigit(): Boolean = ScannerUtils.isBinaryDigit(this)
internal fun Char.isZigOctalDigit(): Boolean = ScannerUtils.isOctalDigit(this)
internal fun Char.isZigDecimalDigit(): Boolean = ScannerUtils.isDecimalDigit(this)
internal fun Char.isZigHexDigit(): Boolean = ScannerUtils.isHexDigit(this)

internal fun Char.zigNewlineWidth(reader: TextReader, offset: CodeUnits = 0): CodeUnits =
	ScannerUtils.getVerticalWhitespaceWidth(this, reader, offset)

internal fun Char.isZigBuiltinIdentifierStart(): Boolean = ScannerUtils.isBuiltinIdentifierStart(this)
internal fun Char.isZigBuiltinIdentifierPart(): Boolean = ScannerUtils.isBuiltinIdentifierPart(this)
internal fun Int.isZigUserIdentifierStart(): Boolean = ScannerUtils.isUserIdentifierStart(this)
internal fun Int.isZigUserIdentifierPart(): Boolean = ScannerUtils.isUserIdentifierPart(this)

internal fun Char.isZigNumberBodyPart(): Boolean = ScannerUtils.isNumberBodyPart(this)

internal fun Char.isZigValidFloatFractionStart(): Boolean = ScannerUtils.isValidFloatFractionStart(this)