package net.landless_city.zigocracy.zig.scanner.impl

import net.landless_city.zigocracy.zig.scanner.ScanResult
import net.landless_city.zigocracy.zig.scanner.TextReader
import net.landless_city.zigocracy.zig.scanner.TokenScanner
import net.landless_city.zigocracy.zig.scanner.util.isZigVerticalWhitespace
import net.landless_city.zigocracy.zig.syntax.TokenKind

object MultilineStringScanner : TokenScanner {
	override fun scan(reader: TextReader): ScanResult {
		// Guard: Must start exactly with "\\"
		val c0 = reader.peekChar(0) ?: return ScanResult.NoMatch
		val c1 = reader.peekChar(1) ?: return ScanResult.NoMatch
		if (c0 != '\\' || c1 != '\\') return ScanResult.NoMatch

		var width = 2 // Consume the opening "\\" prefix

		while (true) {
			val c = reader.peekChar(width)

			if (c == null || c.isZigVerticalWhitespace()) {
				break
			}

			width++
		}

		return ScanResult.Matched(
			TokenKind.MultilineStringPart,
			width,
			diagnostics = emptyList()
		)
	}
}