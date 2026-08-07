package net.landless_city.zigocracy.zig.scanner.impl

import net.landless_city.zigocracy.zig.scanner.ScanResult
import net.landless_city.zigocracy.zig.scanner.Scanner
import net.landless_city.zigocracy.zig.scanner.util.isZigHorizontalWhitespace
import net.landless_city.zigocracy.zig.scanner.util.isZigVerticalWhitespace
import net.landless_city.zigocracy.zig.scanner.util.zigNewlineWidth
import net.landless_city.zigocracy.zig.syntax.TokenKind
import net.landless_city.zigocracy.zig.text.TextReader

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