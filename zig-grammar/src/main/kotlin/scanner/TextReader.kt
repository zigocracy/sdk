package net.landless_city.zigocracy.zig.scanner

import net.landless_city.zigocracy.zig.shared.CodeUnits

/**
 * Exposes a read-only window over the source file content,
 * anchored at the starting boundary of the current token.
 */
interface TextReader {
	/**
	 * Returns the character at the given [offset] from the token start.
	 *
	 * @return The [Char], or `null` if the target position hits EOF.
	 */
	fun peekChar(offset: CodeUnits = 0): Char?

	/**
	 * Slices out a substring of the specified [width] starting from the given [offset].
	 *
	 * @param offset The starting position relative to the token origin.
	 * @param width The maximum number of characters to read.
	 * @return The [String] chunk. If it crosses EOF, only the available text is returned.
	 */
	fun peekString(width: CodeUnits, offset: CodeUnits = 0): String
}