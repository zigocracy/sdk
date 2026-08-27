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

class CommentScannerTest {
	@Nested
	inner class LexerCompetence {
		@Test
		fun `returns NoMatch if text does not start with double forward slash`() {
			val reader = StringTextStream("/hello") // Only one slash
			val result = CommentScanner.scan(reader)

			assertTrue(result is ScanResult.NoMatch) { "Lexer must reject text without a proper comment prefix." }
		}
	}

	@Nested
	inner class HappyPath {
		@Test
		fun `matches regular line comment`() {
			val text = "// ordinary comment"
			val result = scan(text)

			assertEquals(TokenKind.Comment, result.kind)
			assertEquals(text.length, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}

		@Test
		fun `matches documentation comment`() {
			val text = "/// documentation for node"
			val result = scan(text)

			assertEquals(TokenKind.DocComment, result.kind)
			assertEquals(text.length, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}

		@Test
		fun `matches top level module documentation comment`() {
			val text = "//! root file documentation"
			val result = scan(text)

			assertEquals(TokenKind.TopLevelDocComment, result.kind)
			assertEquals(text.length, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}
	}

	@Nested
	inner class LineEndBoundaries {
		@ParameterizedTest(name = "stops consumption exactly before LF newline for prefix '{0}'")
		@ValueSource(strings = ["//", "///", "//!"])
		fun `stops consumption exactly before LF newline`(prefix: String) {
			val text = "$prefix hello\n"
			val result = scan(text)

			val expectedWidth = "$prefix hello".length
			assertEquals(expectedWidth, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}

		@ParameterizedTest(name = "stops consumption exactly before newline #{index}")
		@ValueSource(strings = ["\r\n", "\r"])
		fun `stops consumption exactly before newline`(lineEnding: String) {
			val prefix = "//"
			val text = "$prefix hello$lineEnding"
			val result = scan(text)

			val expectedWidth = "$prefix hello".length
			assertEquals(expectedWidth, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}
	}

	@Nested
	inner class AmbiguityHandling {
		@Test
		fun `downgrades four slashes to regular comment and raises warning`() {
			val text = "//// visual section divider"
			val result = scan(text)

			assertEquals(TokenKind.Comment, result.kind) { "Four slashes must fall back to a regular comment." }
			assertEquals(text.length, result.width)

			val diagnostics = result.diagnostics
			assertEquals(1, diagnostics.size)

			val error = diagnostics[0]
			assertEquals(DiagnosticCode.CommentError.AmbiguousCommentStyle, error.code)
			assertEquals(0, error.offset)
			assertEquals(4, error.width) // Underlines exactly the "////" prefix
		}
	}

	private fun scan(text: String): ScanResult.Matched =
		runScanner(text, CommentScanner)
}