package com.zigocracy.sdk.zig.scanner.impl

import com.zigocracy.sdk.zig.scanner.ScanResult
import com.zigocracy.sdk.zig.scanner.runScanner
import com.zigocracy.sdk.zig.shared.DiagnosticCode
import com.zigocracy.sdk.zig.syntax.TokenKind
import com.zigocracy.sdk.zig.text.impl.StringTextStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class NumberScannerTest {

	@Nested
	inner class LexerCompetence {
		@Test
		fun `returns NoMatch for non digit text`() {
			val reader = StringTextStream("const")
			val result = NumberScanner.scan(reader)

			assertTrue(result is ScanResult.NoMatch) { "Number scanner must reject non-digit inputs." }
		}
	}

	@Nested
	inner class HappyPath {
		@Test
		fun `matches standard decimal integers`() {
			val text = "42"
			val result = scan(text)

			assertEquals(TokenKind.IntegerLiteral, result.kind)
			assertEquals(text.length, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}

		@ParameterizedTest(name = "matches valid numerical literal '{0}'")
		@ValueSource(strings = ["3.1415", "0.", "42.", "1e-5", "0xf.0b11", "1_000_000.00_12"])
		fun `matches valid floating point formats and exponents`(text: String) {
			val result = scan(text)

			assertEquals(TokenKind.FloatLiteral, result.kind)
			assertEquals(text.length, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}
	}

	@Nested
	inner class TokenBoundaries {
		@ParameterizedTest(name = "stops consumption exactly before trailing whitespace in '{0}'")
		@ValueSource(strings = ["42 ", "42 \n", "42\t"])
		fun `stops consumption exactly before trailing whitespace`(text: String) {
			val result = scan(text)

			assertEquals(TokenKind.IntegerLiteral, result.kind)
			assertEquals(2, result.width)
		}

		@Test
		fun `stops consumption exactly before trailing whitespace after dot`() {
			val result = scan("42. \n")

			assertEquals(TokenKind.FloatLiteral, result.kind)
			assertEquals(3, result.width)
		}

		@Test
		fun `stops consumption exactly before range operators`() {
			val result = scan("0..10")

			assertEquals(TokenKind.IntegerLiteral, result.kind)
			assertEquals(1, result.width)
		}

		@ParameterizedTest(name = "stops consumption exactly before trailing operator '{0}'")
		@ValueSource(strings = [".*", ".?"])
		fun `stops consumption exactly before trailing operators`(op: String) {
			val result = scan("42$op")

			assertEquals(TokenKind.IntegerLiteral, result.kind)
			assertEquals(2, result.width)
		}
	}

	@Nested
	inner class DiagnosticsAndRecovery {
		@ParameterizedTest(name = "intercepts empty radix base prefix for '{0}'")
		@ValueSource(strings = ["0b", "0x", "0x ", "0xp0"])
		fun `intercepts empty radix base prefixes`(text: String) {
			val result = scan(text)

			assertEquals(TokenKind.IntegerLiteral, result.kind)
			assertEquals(2, result.width)

			val error = result.diagnostics[0]
			assertEquals(DiagnosticCode.NumberError.MissingDigitAfterBase, error.code)
			assertEquals(0, error.offset)
			assertEquals(2, error.width)
		}

		@Test
		fun `flags leading zero error`() {
			val text = "077"
			val result = scan(text)

			assertEquals(TokenKind.IntegerLiteral, result.kind)
			assertEquals(text.length, result.width)

			val error = result.diagnostics[0]
			assertEquals(DiagnosticCode.NumberError.LeadingZero, error.code)
			assertEquals(0, error.offset)
			assertEquals(2, error.width)
		}

		@ParameterizedTest(name = "greedily consumes invalid digit in '{0}' for recovery")
		@ValueSource(strings = ["0b102", "1e5a"])
		fun `greedily consumes invalid digits for parser recovery`(text: String) {
			val result = scan(text)
			val expectedKind = if (text.contains('e')) TokenKind.FloatLiteral else TokenKind.IntegerLiteral

			assertEquals(expectedKind, result.kind)
			assertEquals(text.length, result.width)

			val error = result.diagnostics[0]
			assertEquals(DiagnosticCode.NumberError.InvalidDigit, error.code)
			assertEquals(text.length - 1, error.offset)
			assertEquals(1, error.width)
		}

		@ParameterizedTest(name = "flags malformed underscore pattern '{0}'")
		@ValueSource(strings = ["42_", "3_.14", "3._14"])
		fun `flags malformed underscores at illegal structural positions`(text: String) {
			val result = scan(text)
			val expectedKind = if (text.contains('.')) TokenKind.FloatLiteral else TokenKind.IntegerLiteral

			assertEquals(expectedKind, result.kind)
			assertEquals(text.length, result.width)

			val error = result.diagnostics[0]
			assertEquals(DiagnosticCode.NumberError.MalformedUnderscore, error.code)
			assertEquals(text.indexOf('_'), error.offset)
			assertEquals(1, error.width)
		}

		@ParameterizedTest(name = "flags missing scale digits after exponent marker in '{0}'")
		@ValueSource(strings = ["0x1p", "1e+"])
		fun `flags missing exponent scale digits on trailing marker or sign`(text: String) {
			val result = scan(text)

			assertEquals(TokenKind.FloatLiteral, result.kind)
			assertEquals(text.length, result.width)

			val error = result.diagnostics[0]
			assertEquals(DiagnosticCode.NumberError.MissingExponentDigits, error.code)
			assertEquals(text.length - 1, error.offset)
			assertEquals(1, error.width)
		}
	}

	private fun scan(text: String): ScanResult.Matched =
		runScanner(text, NumberScanner)
}