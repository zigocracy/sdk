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

class CommentScannerTest {
	@Nested
	inner class LexerCompetence {
		@Test
		fun `returns NoMatch if text does not start with double forward slash`() {
			val reader = StubTextReader("/hello") // Only one slash
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
		@Test
		fun `stops consumption exactly before standard newline`() {
			val text = "// hello\n"
			val result = scan(text)

			assertEquals(8, result.width) // Strictly consumes "// hello" (length 8)
			assertTrue(result.diagnostics.isEmpty())
		}

		@Test
		fun `stops consumption exactly before Windows CRLF`() {
			val text = "// hello\r\n"
			val result = scan(text)

			assertEquals(8, result.width) // Stops right before '\r', leaving it for WhitespaceScanner
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
			assertEquals(0, error.relativeStart)
			assertEquals(4, error.width) // Underlines exactly the "////" prefix
		}
	}

	private fun scan(text: String): ScanResult.Matched =
		runScanner(text, CommentScanner)
}