package net.landless_city.zigocracy.zig.scanner.impl

import net.landless_city.zigocracy.zig.scanner.ScanResult
import net.landless_city.zigocracy.zig.scanner.StubTextReader
import net.landless_city.zigocracy.zig.scanner.runScanner
import net.landless_city.zigocracy.zig.shared.DiagnosticCode
import net.landless_city.zigocracy.zig.syntax.TokenKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class StringScannerTest {
	@Nested
	inner class LexerCompetence {
		@Test
		fun `returns NoMatch if text does not start with double quote`() {
			val reader = StubTextReader("hello")
			val result = StringScanner.scan(reader)

			assertTrue(result is ScanResult.NoMatch) { "Lexer must reject input outside its competence." }
		}
	}

	@Nested
	inner class HappyPath {
		@Test
		fun `matches an empty perfect string literal`() {
			val result = scan("\"\"")
			assertEquals(TokenKind.StringLiteral, result.kind)
			assertEquals(2, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}

		@Test
		fun `matches a perfect happy path string literal`() {
			val result = scan(
				"""
	"hello world"
	""".trimIndent()
			)
			assertEquals(13, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}

		@Test
		fun `matches standard character escape sequences`() {
			val result = scan(
				"""
	"hello\nworld\""
	""".trimIndent()
			)
			assertEquals(16, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}

		@Test
		fun `matches valid hexadecimal byte escapes`() {
			val result = scan(
				"""
	"\x00\xff\x1A"
	""".trimIndent()
			)
			assertEquals(14, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}

		@Test
		fun `matches valid unicode scalar escapes`() {
			val result = scan(
				"""
	"\u{1}\u{1f600}\u{10ffff}"
	""".trimIndent()
			)
			assertEquals(26, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}
	}

	@Nested
	inner class UnterminatedResilience {
		@Test
		fun `handles completely empty unterminated string at EOF`() {
			val result = scan("\"")
			assertEquals(1, result.width)

			val diagnostics = result.diagnostics
			assertEquals(1, diagnostics.size)

			val error = diagnostics[0]
			assertEquals(DiagnosticCode.StringError.UnterminatedString, error.code)
			assertEquals(1, error.relativeStart)
			assertEquals(0, error.width)
		}

		@Test
		fun `handles completely empty unterminated string at LF newline`() {
			val result = scan("\"\n")
			assertEquals(1, result.width)

			val diagnostics = result.diagnostics
			assertEquals(1, diagnostics.size)

			val error = diagnostics[0]
			assertEquals(DiagnosticCode.StringError.UnterminatedString, error.code)
			assertEquals(1, error.relativeStart)
			assertEquals(0, error.width)
		}

		@Test
		fun `handles completely empty unterminated string at CR newline`() {
			val result = scan("\"\r")
			assertEquals(1, result.width)

			val diagnostics = result.diagnostics
			assertEquals(1, diagnostics.size)

			val error = diagnostics[0]
			assertEquals(DiagnosticCode.StringError.UnterminatedString, error.code)
			assertEquals(1, error.relativeStart)
			assertEquals(0, error.width)
		}

		@Test
		fun `handles unterminated string text due to unexpected EOF`() {
			val text = """
				"hello
				""".trimIndent()
			val result = scan(text)

			assertEquals(text.length, result.width)

			val diagnostics = result.diagnostics
			assertEquals(1, diagnostics.size)

			val error = diagnostics[0]
			assertEquals(DiagnosticCode.StringError.UnterminatedString, error.code)
			assertEquals(text.length, error.relativeStart)
			assertEquals(0, error.width)
		}

		@Test
		fun `handles unterminated string text due to LF newline`() {
			val text = """
				"hello
				world"
				""".trimIndent()
			val result = scan(text)

			assertEquals("\"hello".length, result.width)


			val diagnostics = result.diagnostics
			assertEquals(1, diagnostics.size)

			val error = diagnostics[0]
			assertEquals(DiagnosticCode.StringError.UnterminatedString, error.code)
			assertEquals("\"hello".length, error.relativeStart)
			assertEquals(0, error.width)
		}

		@Test
		fun `handles unterminated string text due to CR newline`() {
			val text = "\"hello\r\n"
			val result = scan(text)

			assertEquals(6, result.width)

			val diagnostics = result.diagnostics
			assertEquals(1, diagnostics.size)

			val error = diagnostics[0]
			assertEquals(DiagnosticCode.StringError.UnterminatedString, error.code)
			assertEquals(6, error.relativeStart)
			assertEquals(0, error.width)
		}

		@Test
		fun `handles extreme edge case where string ends with lone backslash at EOF`() {
			val text = "\"hello\\"
			val result = scan(text)

			assertEquals(text.length, result.width)
			assertEquals(DiagnosticCode.StringError.UnterminatedString, result.diagnostics[0].code)
		}
	}

	@Nested
	inner class PanicModeRecovery {
		@Test
		fun `recovers from malformed hex escape without skipping closing double quote`() {
			// In "\x1" the second quote is a synchronizing boundary token.
			// Panic recovery must stop immediately before it, preventing cascade failure.
			val text = """
				"\x1"
				""".trimIndent()
			val result = scan(text)

			assertEquals(5, result.width) { "Lexer must close the string perfectly at the final quote." }


			val diagnostics = result.diagnostics
			assertEquals(1, diagnostics.size)

			val error = diagnostics[0]
			assertEquals(DiagnosticCode.StringError.MalformedHexEscape, error.code)
			assertEquals(1, error.relativeStart) // Points exactly to '\'
			assertEquals(3, error.width) // Covers "\x1" payload length
		}

		@Test
		fun `recovers from malformed unicode escape without skipping closing double quote`() {
			// In "\u{123 hello" the quote must sync-stop the loop.
			val text = """
				"\u{123 hello"
				""".trimIndent()
			val result = scan(text)

			assertEquals(14, result.width) { "Lexer must recover clean at the final quote boundary." }

			val diagnostics = result.diagnostics
			assertEquals(1, diagnostics.size)

			val error = diagnostics[0]
			assertEquals(DiagnosticCode.StringError.MalformedUnicodeEscape, error.code)
			assertEquals(1, error.relativeStart)
			assertEquals(12, error.width) // Covers "\u{123 hello" payload length
		}
	}

	@Nested
	inner class EscapeValidationErrors {
		@Test
		fun `captures an unknown escape sequence while preserving token integrity`() {
			val text = """
				"bad\gseq"
				""".trimIndent()
			val result = scan(text)

			assertEquals(text.length, result.width)

			val diagnostics = result.diagnostics
			assertEquals(1, diagnostics.size)

			val error = diagnostics[0]
			assertEquals(DiagnosticCode.StringError.UnknownEscapeSequence, error.code)
			assertEquals(4, error.relativeStart) // Index of '\'
			assertEquals(2, error.width) // Covers "\g"
		}

		@Test
		fun `captures multiple unknown escape sequences inside a single token`() {
			val text = """
				"\g and \z"
				""".trimIndent()
			val result = scan(text)

			assertEquals(text.length, result.width)

			val diagnostics = result.diagnostics
			assertEquals(2, diagnostics.size)

			val firstError = diagnostics[0]
			assertEquals(DiagnosticCode.StringError.UnknownEscapeSequence, firstError.code)
			val firstSlice = text.substring(firstError.relativeStart, firstError.relativeStart + firstError.width)
			assertEquals("\\g", firstSlice)

			val secondError = diagnostics[1]
			assertEquals(DiagnosticCode.StringError.UnknownEscapeSequence, secondError.code)
			val secondSlice = text.substring(secondError.relativeStart, secondError.relativeStart + secondError.width)
			assertEquals("\\z", secondSlice)
		}
	}

	private fun scan(text: String): ScanResult.Matched =
		runScanner(text, StringScanner)
}