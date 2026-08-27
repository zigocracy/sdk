package net.landless_city.zigocracy.zig.text

/**
 * A read-only interface for relative text access.
 */
interface TextReader {
	/**
	 * Reads the character at the given offset.
	 *
	 * @param offset The position relative to the current cursor.
	 * @return The character, or `null` if the position is out of bounds.
	 */
	fun peekChar(offset: CodeUnits = 0): Char?

	/**
	 * Reads a string segment starting from the given offset.
	 *
	 * If the requested range crosses EOF, the result is clamped to return only the available characters.
	 *
	 * @param width The maximum number of characters to read.
	 * @param offset The starting position relative to the current cursor.
	 * @return The available text slice, or an empty string if the offset is entirely out of bounds.
	 */
	fun peekString(width: CodeUnits, offset: CodeUnits = 0): String
}