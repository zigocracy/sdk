package com.zigocracy.sdk.zig.scanner.impl

import com.zigocracy.sdk.zig.scanner.ScanResult
import com.zigocracy.sdk.zig.scanner.Scanner
import com.zigocracy.sdk.zig.scanner.util.isZigVerticalWhitespace
import com.zigocracy.sdk.zig.syntax.TokenKind
import com.zigocracy.sdk.zig.text.TextReader

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