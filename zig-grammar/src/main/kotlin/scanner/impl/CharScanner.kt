package net.landless_city.zigocracy.zig.scanner.impl

import net.landless_city.zigocracy.zig.scanner.ScanDiagnostics
import net.landless_city.zigocracy.zig.scanner.ScanResult
import net.landless_city.zigocracy.zig.scanner.Scanner
import net.landless_city.zigocracy.zig.scanner.util.EscapeSequence
import net.landless_city.zigocracy.zig.scanner.util.RecoveryContext
import net.landless_city.zigocracy.zig.scanner.util.isZigHorizontalWhitespace
import net.landless_city.zigocracy.zig.scanner.util.isZigVerticalWhitespace
import net.landless_city.zigocracy.zig.shared.DiagnosticCode
import net.landless_city.zigocracy.zig.syntax.TokenKind
import net.landless_city.zigocracy.zig.text.TextReader

/**
 * Like [NumberScanner], this scanner uses a greedy recovery strategy. If a user types
 * an overlong character literal (e.g., 'abc'), we consume the entire alphanumeric block
 * up to the closing single quote.
 */
internal object CharScanner : Scanner {
	override fun scan(reader: TextReader): ScanResult {
		val firstChar = reader.peekChar()!!
		if (firstChar != '\'') return ScanResult.NoMatch

		val diagnostics = mutableListOf<ScanDiagnostics>()
		var width = 1 // Consume opening single quote

		var isOverlong = false
		var charCount = 0

		val context = RecoveryContext.CharLiteral

		while (true) {
			val c = reader.peekChar(width)

			when {
				// 1. End of file or vertical newline boundary reached
				c == null || c.isZigVerticalWhitespace() -> {
					diagnostics.add(ScanDiagnostics(DiagnosticCode.CharError.UnterminatedChar, 0, width))
					return ScanResult.Matched(TokenKind.CharLiteral, width, diagnostics)
				}

				// 2. Character literal successfully closed via its designated terminal character
				c == context.terminalChar -> {
					width++
					if (charCount == 0) {
						diagnostics.add(ScanDiagnostics(DiagnosticCode.CharError.EmptyCharLiteral, 0, width))
					}
					break
				}

				// 3. Delegate escape sequence evaluation
				c == '\\' -> {
					width += EscapeSequence.scan(reader, width, context, diagnostics)
					charCount++
					if (charCount > 1) isOverlong = true
				}

				// 4. Evaluate horizontal whitespace nature against active recovery domain
				c.isZigHorizontalWhitespace() -> {
					if (context.isInteriorWhitespace(reader, base = 0, currentWidth = width)) {
						width++ // Valid interior space enclosed by quote, consume normally
						charCount++
						if (charCount > 1) isOverlong = true
					} else {
						// Trailing whitespace segment heading to a crash boundary. Halt immediately.
						diagnostics.add(ScanDiagnostics(DiagnosticCode.CharError.UnterminatedChar, 0, width))
						return ScanResult.Matched(TokenKind.CharLiteral, width, diagnostics)
					}
				}

				// 5. Standard body character data
				else -> {
					width++
					charCount++
					if (charCount > 1) isOverlong = true
				}
			}
		}

		if (isOverlong) {
			diagnostics.add(
				ScanDiagnostics(
					code = DiagnosticCode.CharError.OverlongCharLiteral,
					offset = 0,
					width = width
				)
			)
		}

		return ScanResult.Matched(
			TokenKind.CharLiteral,
			width,
			diagnostics
		)
	}
}