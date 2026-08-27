package net.landless_city.zigocracy.zig.text

/**
 * A mutable text stream that tracks and advances the current reading position.
 */
interface TextStream : TextReader {
	/**
	 * The current absolute offset from the beginning of the text.
	 */
	val textCursor: CodeUnits

	/**
	 * Advances the reading position strictly monotonically forward by the specified width.
	 *
	 * The width must be positive to ensure the stream cannot stall or move backward.
	 *
	 * @param width The number of code units to consume.
	 */
	fun advance(width: CodeUnits)
}