package net.landless_city.zigocracy.zig.parser

import net.landless_city.zigocracy.zig.scanner.TextReader
import net.landless_city.zigocracy.zig.shared.CodeUnits
import net.landless_city.zigocracy.zig.shared.SourceFile

/**
 * A mutable, monotonic lookahead cursor wrapping a [SourceFile] text space.
 * Acts as the primary stateful bridge feeding raw data streams to token scanners.
 */
class LinearTextCursor(val sourceFile: SourceFile) : TextReader {
	/**
	 * The absolute character offset relative to the beginning of the [SourceFile].
	 */
	var textCursor: CodeUnits = 0
		private set

	override fun peekChar(offset: CodeUnits): Char? {
		val target = textCursor + offset
		if (target >= sourceFile.width || target < 0) return null
		return sourceFile.text[target]
	}

	override fun peekString(width: CodeUnits, offset: CodeUnits): String {
		val startTarget = textCursor + offset
		// Guard against underflow, empty width requests, and immediate EOF overshoot
		if (startTarget >= sourceFile.width || startTarget < 0 || width <= 0) return ""

		// Clamp the slice length to the remaining space to guarantee no array out-of-bounds leaks
		val availableWidth = minOf(width, sourceFile.width - startTarget)
		if (availableWidth <= 0) return ""
		return sourceFile.getTextSlice(startTarget, availableWidth)
	}

	/**
	 * Monotonically advances the cursor forward by the specified token [width].
	 *
	 * @param width The physical length of the consumed token.
	 * @throws IllegalArgumentException If [width] is non-positive, protecting the pipeline against infinite loop stalls.
	 */
	fun advance(width: CodeUnits) {
		require(width > 0) { "Cursor must move forward." }
		textCursor += width
	}
}