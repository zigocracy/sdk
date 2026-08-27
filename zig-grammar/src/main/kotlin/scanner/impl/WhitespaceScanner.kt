package com.zigocracy.sdk.zig.scanner.impl

import com.zigocracy.sdk.zig.scanner.ScanResult
import com.zigocracy.sdk.zig.scanner.Scanner
import com.zigocracy.sdk.zig.scanner.util.isZigHorizontalWhitespace
import com.zigocracy.sdk.zig.scanner.util.isZigVerticalWhitespace
import com.zigocracy.sdk.zig.scanner.util.zigNewlineWidth
import com.zigocracy.sdk.zig.syntax.TokenKind
import com.zigocracy.sdk.zig.text.TextReader

internal object WhitespaceScanner : Scanner {
	override fun scan(reader: TextReader): ScanResult {
		val firstChar = reader.peekChar()!!

		when {
			firstChar.isZigHorizontalWhitespace() -> {
				// Greedily groups contiguous horizontal spaces or tabs
				var width = 1
				while (reader.peekChar(width)?.isZigHorizontalWhitespace() == true) {
					width++
				}

				return ScanResult.Matched(
					TokenKind.Whitespace,
					width,
					diagnostics = emptyList()
				)
			}

			firstChar.isZigVerticalWhitespace() -> {
				val width = firstChar.zigNewlineWidth(reader)
				return ScanResult.Matched(
					TokenKind.Newline,
					width,
					diagnostics = emptyList()
				)
			}

			else -> return ScanResult.NoMatch
		}
	}
}