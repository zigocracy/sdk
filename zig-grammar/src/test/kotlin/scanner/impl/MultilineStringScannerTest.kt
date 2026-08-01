package net.landless_city.zigocracy.zig.scanner.impl

import net.landless_city.zigocracy.zig.scanner.ScanResult
import net.landless_city.zigocracy.zig.scanner.StubTextReader
import net.landless_city.zigocracy.zig.scanner.runScanner
import net.landless_city.zigocracy.zig.syntax.TokenKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MultilineStringScannerTest {
	@Nested
	inner class LexerCompetence {
		@Test
		fun `returns NoMatch if text does not start with double backslash`() {
			val reader = StubTextReader(
				"""
	\hello
	""".trimIndent()
			) // Only one backslash
			val result = MultilineStringScanner.scan(reader)
			assertTrue(result is ScanResult.NoMatch)
		}
	}

	@Nested
	inner class HappyPath {
		@Test
		fun `matches an empty multiline string literal`() {
			val result = scan("\\\\")
			assertEquals(TokenKind.MultilineStringPart, result.kind)
			assertEquals(2, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}

		@Test
		fun `matches a line string with raw text and ignores potential escape characters`() {
			// In multiline strings, backslashes are just raw text bytes, not escape sequences.
			val text = """
				\\hello\nworld\x00
				""".trimIndent()
			val result = scan(text)
			assertEquals(text.length, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}
	}

	@Nested
	inner class LineEndBoundaries {
		@Test
		fun `stops consumption exactly before a LF newline`() {
			val text = "\\\\hello\n"
			val result = scan(text)
			assertEquals(7, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}

		@Test
		fun `stops consumption exactly before a CR newline`() {
			val text = "\\\\hello\r"
			val result = scan(text)
			assertEquals(7, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}
	}

	private fun scan(text: String): ScanResult.Matched =
		runScanner(text, MultilineStringScanner)
}