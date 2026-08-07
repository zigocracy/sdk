package net.landless_city.zigocracy.zig.scanner.impl

import net.landless_city.zigocracy.zig.scanner.ScanResult
import net.landless_city.zigocracy.zig.scanner.runScanner
import net.landless_city.zigocracy.zig.syntax.TokenKind
import net.landless_city.zigocracy.zig.text.impl.StringTextStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class MultilineStringScannerTest {

	@Nested
	inner class LexerCompetence {
		@Test
		fun `returns NoMatch if text does not start with double backslash`() {
			val reader = StringTextStream("\\hello") // Only one backslash
			val result = MultilineStringScanner.scan(reader)

			assertTrue(result is ScanResult.NoMatch) { "Multiline string scanner must reject text without '\\\\' prefix." }
		}
	}

	@Nested
	inner class HappyPath {
		@Test
		fun `matches empty multiline string literal`() {
			val text = "\\\\"
			val result = scan(text)

			assertEquals(TokenKind.MultilineStringPart, result.kind)
			assertEquals(text.length, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}

		@Test
		fun `matches multiline string part containing raw text without evaluating escape characters`() {
			val text = """
				\\hello\nworld\x00
				""".trimIndent()
			val result = scan(text)

			assertEquals(TokenKind.MultilineStringPart, result.kind)
			assertEquals(text.length, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}
	}

	@Nested
	inner class LineEndBoundaries {
		@ParameterizedTest(name = "stops consumption exactly before newline #{index}")
		@ValueSource(strings = ["\n", "\r\n", "\r"])
		fun `stops consumption exactly before newline`(lineEnding: String) {
			val prefix = "\\\\hello"
			val text = "$prefix$lineEnding"
			val result = scan(text)

			assertEquals(prefix.length, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}
	}

	private fun scan(text: String): ScanResult.Matched =
		runScanner(text, MultilineStringScanner)
}