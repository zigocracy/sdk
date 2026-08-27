package com.zigocracy.sdk.zig.text.impl

import com.zigocracy.sdk.zig.text.CodeUnits
import com.zigocracy.sdk.zig.text.SourceFile
import com.zigocracy.sdk.zig.text.TextStream

/**
 * A safe, mutable text stream implementation backed by a [SourceFile].
 */
internal class SourceFileTextStream(private val sourceFile: SourceFile) : TextStream {
	override var textCursor: CodeUnits = 0
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

	override fun advance(width: CodeUnits) {
		require(width > 0) { "Cursor must move forward. Attempted to advance by: $width" }
		textCursor += width
	}
}
