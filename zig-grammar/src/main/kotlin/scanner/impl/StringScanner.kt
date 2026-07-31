package net.landless_city.zigocracy.zig.scanner.impl

import net.landless_city.zigocracy.zig.scanner.ScanResult
import net.landless_city.zigocracy.zig.scanner.TextReader
import net.landless_city.zigocracy.zig.scanner.TokenDiagnostic
import net.landless_city.zigocracy.zig.scanner.TokenScanner
import net.landless_city.zigocracy.zig.scanner.util.EscapeSequence
import net.landless_city.zigocracy.zig.scanner.util.RecoveryContext
import net.landless_city.zigocracy.zig.scanner.util.isZigHorizontalWhitespace
import net.landless_city.zigocracy.zig.scanner.util.isZigVerticalWhitespace
import net.landless_city.zigocracy.zig.shared.CodeUnits
import net.landless_city.zigocracy.zig.shared.DiagnosticCode
import net.landless_city.zigocracy.zig.syntax.TokenKind

object StringScanner : TokenScanner {
	override fun scan(reader: TextReader): ScanResult {
		val firstChar = reader.peekChar(0) ?: return ScanResult.NoMatch
		if (firstChar != '"') return ScanResult.NoMatch

		val localDiagnostics = mutableListOf<TokenDiagnostic>()
		val totalWidth = scanStringBody(reader, base = 0, localDiagnostics)

		return ScanResult.Matched(
			kind = TokenKind.StringLiteral,
			width = totalWidth,
			diagnostics = localDiagnostics
		)
	}

	internal fun scanStringBody(
		reader: TextReader,
		base: CodeUnits,
		diagnostics: MutableList<TokenDiagnostic>
	): CodeUnits {
		var width = 1
		val context = RecoveryContext.StringLiteral

		while (true) {
			val c = reader.peekChar(base + width)

			when {
				// Examples of unclosed literal termination bounds:
				// - `"hello\n` -> Hits vertical space at width 6; emits a Point-Diagnostic (width 0) at index (base + 6) and halts execution.
				// - `"abc`     -> Hits EOF boundary; creates a trailing Point-Diagnostic at the end of the text.
				c == null || c.isZigVerticalWhitespace() -> {
					diagnostics.add(
						TokenDiagnostic(
							code = DiagnosticCode.StringError.UnterminatedString,
							relativeStart = base + width,
							width = 0
						)
					)
					break
				}

				// Examples of escape payload evaluation boundaries:
				// - `"a\nb"`    -> Passes processing to EscapeSequence sub-system; securely updates width without local pointer desynchronization.
				// - `"\u{123}"` -> Handles multi-character Unicode chunk; returns total evaluated sub-width atomically.
				c == '\\' -> {
					width += EscapeSequence.scan(reader, base = base + width, context, diagnostics)
				}

				// Examples of complete execution paths:
				// - `""`    -> Matches immediately after opening quote, advancing width to 2.
				// - `"foo"` -> Successfully matches closing terminal char, finalizing token construction.
				c == context.terminalChar -> {
					width++
					break
				}

				// Examples of horizontal space routing checks:
				// - `"text "`   -> `isInteriorWhitespace()` returns true since characters exist before closing quote; space is consumed as code payload.
				// - `"bad \n`   -> Space directly flags a line crash dead-end; returns false to freeze width before leaking spaces into the string token.
				c.isZigHorizontalWhitespace() -> {
					if (context.isInteriorWhitespace(reader, base, width)) {
						width++
					} else {
						diagnostics.add(
							TokenDiagnostic(
								code = DiagnosticCode.StringError.UnterminatedString,
								relativeStart = base + width,
								width = 0
							)
						)
						return width
					}
				}

				// Matches standard literal text stream characters safely
				else -> {
					width++
				}
			}
		}

		return width
	}
}