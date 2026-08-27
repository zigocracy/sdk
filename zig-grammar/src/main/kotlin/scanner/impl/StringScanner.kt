package com.zigocracy.sdk.zig.scanner.impl

import com.zigocracy.sdk.zig.scanner.ScanDiagnostics
import com.zigocracy.sdk.zig.scanner.ScanResult
import com.zigocracy.sdk.zig.scanner.Scanner
import com.zigocracy.sdk.zig.scanner.util.EscapeSequence
import com.zigocracy.sdk.zig.scanner.util.RecoveryContext
import com.zigocracy.sdk.zig.scanner.util.isZigHorizontalWhitespace
import com.zigocracy.sdk.zig.scanner.util.isZigVerticalWhitespace
import com.zigocracy.sdk.zig.shared.DiagnosticCode
import com.zigocracy.sdk.zig.syntax.TokenKind
import com.zigocracy.sdk.zig.text.CodeUnits
import com.zigocracy.sdk.zig.text.TextReader

internal object StringScanner : Scanner {
	override fun scan(reader: TextReader): ScanResult {
		val firstChar = reader.peekChar()!!
		if (firstChar != '"') return ScanResult.NoMatch

		val localDiagnostics = mutableListOf<ScanDiagnostics>()
		val totalWidth = scanStringBody(reader, base = 0, localDiagnostics)

		return ScanResult.Matched(
			TokenKind.StringLiteral,
			totalWidth,
			localDiagnostics
		)
	}

	/**
	 * Scans the character sequence inside a string literal, including escape sequences
	 * and internal horizontal whitespaces.
	 *
	 * Stops immediately before encountering a vertical whitespace boundary or EOF,
	 * generating a point-diagnostic (width = 0) at the termination boundary if unclosed.
	 *
	 * @param reader The text stream viewer.
	 * @param base The relative offset where this string payload block starts.
	 * @param diagnostics The target list to collect fine-grained syntax errors.
	 * @return The total width consumed by the string payload.
	 */
	fun scanStringBody(
		reader: TextReader,
		base: CodeUnits,
		diagnostics: MutableList<ScanDiagnostics>
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
						ScanDiagnostics(
							code = DiagnosticCode.StringError.UnterminatedString,
							offset = base + width,
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
							ScanDiagnostics(
								code = DiagnosticCode.StringError.UnterminatedString,
								offset = base + width,
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