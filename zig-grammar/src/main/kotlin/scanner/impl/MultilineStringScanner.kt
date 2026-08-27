package net.landless_city.zigocracy.zig.scanner.impl

import net.landless_city.zigocracy.zig.scanner.ScanResult
import net.landless_city.zigocracy.zig.scanner.Scanner
import net.landless_city.zigocracy.zig.scanner.util.isZigVerticalWhitespace
import net.landless_city.zigocracy.zig.syntax.TokenKind
import net.landless_city.zigocracy.zig.text.TextReader

internal object MultilineStringScanner : Scanner {
	override fun scan(reader: TextReader): ScanResult {
		// Guard: Must start exactly with "\\"
		val c0 = reader.peekChar()!!
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