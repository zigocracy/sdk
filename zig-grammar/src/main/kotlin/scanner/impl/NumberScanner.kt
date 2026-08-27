package com.zigocracy.sdk.zig.scanner.impl

import com.zigocracy.sdk.zig.scanner.ScanDiagnostics
import com.zigocracy.sdk.zig.scanner.ScanResult
import com.zigocracy.sdk.zig.scanner.Scanner
import com.zigocracy.sdk.zig.scanner.util.NumberAccumulator
import com.zigocracy.sdk.zig.scanner.util.NumberBase
import com.zigocracy.sdk.zig.scanner.util.isZigDecimalDigit
import com.zigocracy.sdk.zig.shared.DiagnosticCode
import com.zigocracy.sdk.zig.syntax.TokenKind
import com.zigocracy.sdk.zig.text.TextReader

internal object NumberScanner : Scanner {
	override fun scan(reader: TextReader): ScanResult {
		val firstChar = reader.peekChar()!!
		if (!firstChar.isZigDecimalDigit()) return ScanResult.NoMatch

		val localDiagnostics = mutableListOf<ScanDiagnostics>()

		// Step 1: Identify radix prefix and initial scanner step bounds
		val (base, initialWidth) = resolveNumberBase(reader, firstChar, localDiagnostics)
		var width = initialWidth

		// Intercept empty base payloads right at the prefix boundary
		if (base != NumberBase.Decimal) {
			val nextChar = reader.peekChar(width)
			if (nextChar == null || !base.isValidDigit(nextChar)) {
				localDiagnostics.add(ScanDiagnostics(DiagnosticCode.NumberError.MissingDigitAfterBase, offset = 0, width = width))
				return ScanResult.Matched(TokenKind.IntegerLiteral, width, localDiagnostics)
			}
		}

		// Step 2: Accumulate numerical stream characters
		val numberAccumulator = NumberAccumulator(reader, base, startWidth = width)
		numberAccumulator.accumulate(localDiagnostics)

		width = numberAccumulator.currentWidth

		// Phase 3: Post-scan structural text validation
		val text = reader.peekString(width)
		validateNumberLiteral(text, base, localDiagnostics)

		val kind = if (numberAccumulator.isFloat) TokenKind.FloatLiteral else TokenKind.IntegerLiteral

		return ScanResult.Matched(
			kind,
			width,
			localDiagnostics
		)
	}

	private fun resolveNumberBase(
		reader: TextReader,
		firstChar: Char,
		diagnostics: MutableList<ScanDiagnostics>
	): Pair<NumberBase, Int> {
		if (firstChar != '0') return NumberBase.Decimal to 1

		// Examples of radix evaluation routing:
		// - "0b11" -> Resolves Binary base, updates initialWidth to 2.
		// - "077"  -> Resolves Decimal base, initialWidth 1. Emits LeadingZero diagnostic on the "07" segment.
		// - "0.5"  -> Resolves Decimal base, initialWidth 1. The dot is evaluated inside the accumulator block.
		return when (val next = reader.peekChar(1)) {
			'b', 'B' -> NumberBase.Binary to 2
			'o', 'O' -> NumberBase.Octal to 2
			'x', 'X' -> NumberBase.Hexadecimal to 2
			'.', 'e', 'E', null -> NumberBase.Decimal to 1
			else -> {
				if (next.isZigDecimalDigit()) {
					diagnostics.add(ScanDiagnostics(DiagnosticCode.NumberError.LeadingZero, offset = 0, width = 2))
				}
				NumberBase.Decimal to 1
			}
		}
	}

	private fun validateNumberLiteral(text: String, base: NumberBase, diagnostics: MutableList<ScanDiagnostics>) {
		if (text.isEmpty()) return

		val startOffset = if (base == NumberBase.Decimal) 0 else 2

		// Examples of trailing underscore violations:
		// - "42_"   -> Triggers MalformedUnderscore diagnostic exactly at the final index.
		if (text.last() == '_') {
			diagnostics.add(
				ScanDiagnostics(
					code = DiagnosticCode.NumberError.MalformedUnderscore, offset = text.length - 1, width = 1
				)
			)
		}

		var inExponentMode = false
		var exponentMarkerIndex = -1

		for (i in startOffset until text.length) {
			val c = text[i]

			if (base.isExponentMarker(c)) {
				inExponentMode = true
				exponentMarkerIndex = i
				continue
			}

			if (inExponentMode && (c == '+' || c == '-') && i == exponentMarkerIndex + 1) {
				continue
			}

			// Examples of point-isolated underscore violations:
			// - "3_.14" -> Flags index (i - 1) as MalformedUnderscore.
			// - "3._14" -> Flags index (i + 1) as MalformedUnderscore.
			if (c == '.') {
				if (i > 0 && text[i - 1] == '_') {
					diagnostics.add(ScanDiagnostics(DiagnosticCode.NumberError.MalformedUnderscore, offset = i - 1, width = 1))
				}
				if (i < text.length - 1 && text[i + 1] == '_') {
					diagnostics.add(ScanDiagnostics(DiagnosticCode.NumberError.MalformedUnderscore, offset = i + 1, width = 1))
				}
				continue
			}

			// Examples of invalid digit violations:
			// - "0b102"  -> Decimal '2' flags an InvalidDigit diagnostic under Binary radix context.
			// - "1e5a"   -> Character 'a' flags an InvalidDigit diagnostic since exponents strictly accept only Decimal digits.
			val isValid = if (inExponentMode) {
				c.isZigDecimalDigit() || c == '_'
			} else {
				base.isValidCharacterInLiteral(c)
			}

			if (!isValid) {
				diagnostics.add(
					ScanDiagnostics(
						code = DiagnosticCode.NumberError.InvalidDigit, offset = i, width = 1
					)
				)
			}
		}

		// Examples of missing exponent scale violations:
		// - "1e+"  -> Trailing sign with no digits flags MissingExponentDigits.
		// - "0x1p" -> Trailing marker with no scale payload flags MissingExponentDigits.
		if (inExponentMode) {
			val lastChar = text.last()
			val isTrailingMarker = base.isExponentMarker(lastChar)
			val isTrailingSign = (lastChar == '+' || lastChar == '-') && text.length > 1 && base.isExponentMarker(text[text.length - 2])

			if (isTrailingMarker || isTrailingSign) {
				diagnostics.add(
					ScanDiagnostics(
						code = DiagnosticCode.NumberError.MissingExponentDigits,
						offset = text.length - 1,
						width = 1
					)
				)
			}
		}
	}
}