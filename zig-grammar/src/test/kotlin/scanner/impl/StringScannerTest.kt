package net.landless_city.zigocracy.zig.scanner.impl

import net.landless_city.zigocracy.zig.scanner.ScanResult
import net.landless_city.zigocracy.zig.scanner.runScanner
import net.landless_city.zigocracy.zig.shared.DiagnosticCode
import net.landless_city.zigocracy.zig.syntax.TokenKind
import net.landless_city.zigocracy.zig.text.impl.StringTextStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class StringScannerTest {

	@Nested
	inner class LexerCompetence {
		@Test
		fun `returns NoMatch if text does not start with double quote`() {
			val reader = StringTextStream("hello")
			val result = StringScanner.scan(reader)

			assertTrue(result is ScanResult.NoMatch) { "Lexer must reject input outside its competence." }
		}
	}

	@Nested
	inner class HappyPath {
		@Test
		fun `matches empty string literal`() {
			val text = "\"\""
			val result = scan(text)

			assertEquals(TokenKind.StringLiteral, result.kind)
			assertEquals(text.length, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}

		@ParameterizedTest(name = "matches valid plain string '{0}'")
		@ValueSource(strings = ["\"hello world\"", "\"привет мир\"", "\"测\""])
		fun `matches plain text string literals`(text: String) {
			val result = scan(text)

			assertEquals(TokenKind.StringLiteral, result.kind)
			assertEquals(text.length, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}

		@Test
		fun `matches standard character escape sequences`() {
			val text = """
				"hello\nworld\""
				""".trimIndent()
			val result = scan(text)

			assertEquals(TokenKind.StringLiteral, result.kind)
			assertEquals(text.length, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}

		@Test
		fun `matches fixed length hexadecimal escapes`() {
			val text = """
				"\x00\xff\x1A"
				""".trimIndent()
			val result = scan(text)

			assertEquals(TokenKind.StringLiteral, result.kind)
			assertEquals(text.length, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}

		@Test
		fun `matches variable length unicode escapes`() {
			val text = """
				"\u{1}\u{1f600}\u{10ffff}"
				""".trimIndent()
			val result = scan(text)

			assertEquals(TokenKind.StringLiteral, result.kind)
			assertEquals(text.length, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}
	}

	@Nested
	inner class LineEndBoundaries {
		@ParameterizedTest(name = "stops consumption exactly before newline '{0}'")
		@ValueSource(strings = ["\n", "\r", "\r\n"])
		fun `stops consumption exactly before newline on empty unclosed quote`(lineEnding: String) {
			val prefix = "\""
			val text = "$prefix$lineEnding"
			val result = scan(text)

			assertEquals(prefix.length, result.width)

			val error = result.diagnostics[0]
			assertEquals(DiagnosticCode.StringError.UnterminatedString, error.code)
			assertEquals(prefix.length, error.offset)
			assertEquals(0, error.width)
		}

		@ParameterizedTest(name = "stops consumption exactly before newline '{0}'")
		@ValueSource(strings = ["\n", "\r", "\r\n"])
		fun `stops consumption exactly before newline on unclosed populated string`(lineEnding: String) {
			val prefix = "\"hello"
			val text = "$prefix$lineEnding"
			val result = scan(text)

			assertEquals(prefix.length, result.width)

			val error = result.diagnostics[0]
			assertEquals(DiagnosticCode.StringError.UnterminatedString, error.code)
			assertEquals(prefix.length, error.offset)
			assertEquals(0, error.width)
		}

		@Test
		fun `stops consumption exactly at EOF when trailing character is a lone backslash`() {
			val text = "\"hello\\"
			val result = scan(text)

			assertEquals(text.length, result.width)

			val error = result.diagnostics[0]
			assertEquals(DiagnosticCode.StringError.UnterminatedString, error.code)
		}
	}

	@Nested
	inner class MalformedHandling {
		@Test
		fun `flags unknown escape sequence`() {
			val text = """
				"bad\gseq"
				""".trimIndent()
			val result = scan(text)

			assertEquals(text.length, result.width)

			val error = result.diagnostics[0]
			assertEquals(DiagnosticCode.StringError.UnknownEscapeSequence, error.code)
			assertEquals(text.indexOf('\\'), error.offset)
			assertEquals(2, error.width)
		}

		@Test
		fun `flags multiple sequential unknown escape sequences`() {
			val text = """
				"\g and \z"
				""".trimIndent()
			val result = scan(text)

			assertEquals(text.length, result.width)

			val diagnostics = result.diagnostics
			assertEquals(2, diagnostics.size)

			val firstError = diagnostics[0]
			assertEquals(DiagnosticCode.StringError.UnknownEscapeSequence, firstError.code)
			assertEquals(text.indexOf("\\g"), firstError.offset)
			assertEquals(2, firstError.width)

			val secondError = diagnostics[1]
			assertEquals(DiagnosticCode.StringError.UnknownEscapeSequence, secondError.code)
			assertEquals(text.indexOf("\\z"), secondError.offset)
			assertEquals(2, secondError.width)
		}

		@Test
		fun `intercepts malformed fixed length hex escape before closing quote`() {
			val text = """
				"\x1"
				""".trimIndent()
			val result = scan(text)

			assertEquals(text.length, result.width)

			val error = result.diagnostics[0]
			assertEquals(DiagnosticCode.StringError.MalformedHexEscape, error.code)
			assertEquals(text.indexOf('\\'), error.offset)
			assertEquals(3, error.width)
		}

		@Test
		fun `intercepts malformed variable length unicode escape before closing quote`() {
			val text = """
				"\u{123 hello"
				""".trimIndent()
			val result = scan(text)

			assertEquals(text.length, result.width)

			val error = result.diagnostics[0]
			assertEquals(DiagnosticCode.StringError.MalformedUnicodeEscape, error.code)
			assertEquals(text.indexOf('\\'), error.offset)
			assertEquals(12, error.width)
		}
	}

	private fun scan(text: String): ScanResult.Matched =
		runScanner(text, StringScanner)
}