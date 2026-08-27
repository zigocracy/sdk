package com.zigocracy.sdk.zig.text.impl

import com.zigocracy.sdk.zig.text.CodeUnits
import com.zigocracy.sdk.zig.text.TextStream

/**
 * A safe, mutable text stream implementation backed directly by a raw [String].
 */
class StringTextStream(private val text: String) : TextStream {
	override var textCursor: CodeUnits = 0
		private set

	override fun peekChar(offset: CodeUnits): Char? {
		val target = textCursor + offset
		if (target >= text.length || target < 0) return null
		return text[target]
	}

	override fun peekString(width: CodeUnits, offset: CodeUnits): String {
		val startTarget = textCursor + offset
		// Guard against underflow, empty width requests, and immediate EOF overshoot
		if (startTarget >= text.length || startTarget < 0 || width <= 0) return ""

		// Clamp the slice length to the remaining space to guarantee no array out-of-bounds leaks
		val availableWidth = minOf(width, text.length - startTarget)
		if (availableWidth <= 0) return ""

		return text.substring(startTarget, startTarget + availableWidth)
	}

	override fun advance(width: CodeUnits) {
		require(width > 0) { "Cursor must move forward. Attempted to advance by: $width" }
		textCursor += width
	}
}