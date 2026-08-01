package net.landless_city.zigocracy.zig.scanner.impl

import net.landless_city.zigocracy.zig.scanner.ScanResult
import net.landless_city.zigocracy.zig.scanner.StubTextReader
import net.landless_city.zigocracy.zig.scanner.runScanner
import net.landless_city.zigocracy.zig.syntax.TokenKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PunctuationScannerTest {
	@Nested
	inner class LexerCompetence {
		@Test
		fun `returns NoMatch for alphanumeric characters`() {
			val reader = StubTextReader("const")
			val result = PunctuationScanner.scan(reader)

			assertTrue(result is ScanResult.NoMatch) { "Scanner must reject alphanumeric text." }
		}

		@Test
		fun `returns NoMatch for empty text`() {
			val reader = StubTextReader("")
			val result = PunctuationScanner.scan(reader)

			assertTrue(result is ScanResult.NoMatch)
		}
	}

	@Nested
	inner class MaximalMunchGreedyMatching {
		@Test
		fun `greedily matches longest four character token`() {
			val result = scan("<<|=")
			assertEquals(TokenKind.ShlSaturateAssign, result.kind)
			assertEquals(4, result.width)
		}

		@Test
		fun `greedily matches three character token and ignores shorter prefixes`() {
			val result = scan("...")
			assertEquals(TokenKind.Ellipsis, result.kind)
			assertEquals(3, result.width)
		}

		@Test
		fun `greedily matches two character compound operator`() {
			val result = scan("==")
			assertEquals(TokenKind.Equals, result.kind)
			assertEquals(2, result.width)
		}

		@Test
		fun `matches unique zig suffix operators`() {
			val resultAsterisk = scan(".*")
			assertEquals(TokenKind.PtrDereference, resultAsterisk.kind)
			assertEquals(2, resultAsterisk.width)

			val resultQuestion = scan(".?")
			assertEquals(TokenKind.OptionalUnwrap, resultQuestion.kind)
			assertEquals(2, resultQuestion.width)
		}

		@Test
		fun `falls back to single character token when compound match is impossible`() {
			val result = scan("=+")
			assertEquals(TokenKind.Assign, result.kind)
			assertEquals(1, result.width)
		}
	}

	@Nested
	inner class UniformCharacterCoverage {
		@Test
		fun `matches standard punctuation boundaries`() {
			assertEquals(TokenKind.LeftParen, scan("(").kind)
			assertEquals(TokenKind.RightParen, scan(")").kind)
			assertEquals(TokenKind.LeftBrace, scan("{").kind)
			assertEquals(TokenKind.RightBrace, scan("}").kind)
			assertEquals(TokenKind.Semicolon, scan(";").kind)
			assertEquals(TokenKind.Comma, scan(",").kind)
		}

		@Test
		fun `matches standalone bitwise operators`() {
			assertEquals(TokenKind.BitwiseAnd, scan("&").kind)
			assertEquals(TokenKind.BitwiseOr, scan("|").kind)
			assertEquals(TokenKind.BitwiseXor, scan("^").kind)
		}
	}

	private fun scan(text: String): ScanResult.Matched =
		runScanner(text, PunctuationScanner)
}