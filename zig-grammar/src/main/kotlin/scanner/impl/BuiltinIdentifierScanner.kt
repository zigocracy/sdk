package com.zigocracy.sdk.zig.scanner.impl

import com.zigocracy.sdk.zig.scanner.ScanResult
import com.zigocracy.sdk.zig.scanner.Scanner
import com.zigocracy.sdk.zig.scanner.util.isZigBuiltinIdentifierPart
import com.zigocracy.sdk.zig.scanner.util.isZigBuiltinIdentifierStart
import com.zigocracy.sdk.zig.syntax.TokenKind
import com.zigocracy.sdk.zig.text.TextReader

internal object BuiltinIdentifierScanner : Scanner {
	override fun scan(reader: TextReader): ScanResult {
		val firstChar = reader.peekChar()!!
		if (firstChar != '@') return ScanResult.NoMatch

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
