package net.landless_city.zigocracy.zig.scanner.impl

import net.landless_city.zigocracy.zig.scanner.ScanResult
import net.landless_city.zigocracy.zig.scanner.TextReader
import net.landless_city.zigocracy.zig.scanner.TokenScanner
import net.landless_city.zigocracy.zig.scanner.util.isZigBuiltinIdentifierPart
import net.landless_city.zigocracy.zig.scanner.util.isZigBuiltinIdentifierStart
import net.landless_city.zigocracy.zig.syntax.TokenKind

object BuiltinIdentiiferScanner : TokenScanner {
	override fun scan(reader: TextReader): ScanResult {
		if (reader.peekChar(0) != '@') return ScanResult.NoMatch

		val next = reader.peekChar(1) ?: return ScanResult.NoMatch
		if (!next.isZigBuiltinIdentifierStart()) return ScanResult.NoMatch

		var width = 2

		while (true) {
			val c = reader.peekChar(width)
			if (c == null || !c.isZigBuiltinIdentifierPart()) {
				break
			}
			width++
		}

		return ScanResult.Matched(
			TokenKind.BuiltinIdentifier,
			width,
			diagnostics = emptyList()
		)
	}
}
