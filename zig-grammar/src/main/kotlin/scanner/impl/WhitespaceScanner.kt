package net.landless_city.zigocracy.zig.scanner.impl

import net.landless_city.zigocracy.zig.scanner.ScanResult
import net.landless_city.zigocracy.zig.scanner.TextReader
import net.landless_city.zigocracy.zig.scanner.TokenScanner
import net.landless_city.zigocracy.zig.scanner.util.isZigHorizontalWhitespace
import net.landless_city.zigocracy.zig.scanner.util.isZigVerticalWhitespace
import net.landless_city.zigocracy.zig.scanner.util.zigNewlineWidth
import net.landless_city.zigocracy.zig.syntax.TokenKind

object WhitespaceScanner : TokenScanner {
	override fun scan(reader: TextReader): ScanResult {
		val firstChar = reader.peekChar() ?: return ScanResult.NoMatch

		when {
			// Examples of horizontal trivia accumulation:
			// - "   foo" -> Iteration accumulates spaces and halts right before 'f', returning length 3.
			// - "\t\tbar" -> Accumulates consecutive tabs and preserves them in a single structural token chunk.
			firstChar.isZigHorizontalWhitespace() -> {
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

			// Examples of multi-character newline translation:
			// - "\n"   -> `zigNewlineWidth` resolves as a single Unix CodeUnit (width 1).
			// - "\r\n"  -> `zigNewlineWidth` identifies the Windows sequence pair and returns width 2 atomically.
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