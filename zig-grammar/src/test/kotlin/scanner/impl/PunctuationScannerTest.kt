package com.zigocracy.sdk.zig.scanner.impl

import com.zigocracy.sdk.zig.scanner.ScanResult
import com.zigocracy.sdk.zig.scanner.runScanner
import com.zigocracy.sdk.zig.syntax.TokenKind
import com.zigocracy.sdk.zig.text.impl.StringTextStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource

class PunctuationScannerTest {

	@Nested
	inner class LexerCompetence {
		@Test
		fun `returns NoMatch for alphanumeric characters`() {
			val reader = StringTextStream("const")
			val result = PunctuationScanner.scan(reader)

			assertTrue(result is ScanResult.NoMatch) { "Punctuation scanner must reject alphanumeric text." }
		}

		@Test
		fun `returns NoMatch for empty text`() {
			val reader = StringTextStream("")
			val result = PunctuationScanner.scan(reader)

			assertTrue(result is ScanResult.NoMatch)
		}
	}

	@Nested
	inner class HappyPath {
		@ParameterizedTest(name = "matches operator '{0}' with correct TokenKind")
		@MethodSource("com.zigocracy.sdk.zig.scanner.impl.PunctuationScannerTest#representativePunctuationProvider")
		fun `greedily matches punctuation symbols`(text: String, expectedKind: TokenKind) {
			val result = scan(text)

			assertEquals(expectedKind, result.kind)
			assertEquals(text.length, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}
	}

	@Nested
	inner class TokenBoundaries {
		@ParameterizedTest(name = "stops consumption exactly before alphanumeric tail '{0}'")
		@ValueSource(strings = ["my_variable", "42.0", "123"])
		fun `stops consumption exactly before alphanumeric characters`(tail: String) {
			val prefix = "="
			val text = "$prefix$tail"
			val result = scan(text)

			assertEquals(prefix.length, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}

		@ParameterizedTest(name = "stops consumption exactly before operator tail '{0}'")
		@ValueSource(strings = ["+", "-", "*", "/"])
		fun `stops consumption exactly before standalone operators`(tail: String) {
			val prefix = "<<|="
			val text = "$prefix$tail"
			val result = scan(text)

			assertEquals(prefix.length, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}

		@ParameterizedTest(name = "stops consumption exactly before operator tail '{0}'")
		@ValueSource(strings = [".*", ".?"])
		fun `stops consumption exactly before compound operators`(tail: String) {
			val prefix = ".*"
			val text = "$prefix$tail"
			val result = scan(text)

			assertEquals(prefix.length, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}

		@Test
		fun `stops consumption exactly before dot character`() {
			val prefix = "..."
			val text = "$prefix."
			val result = scan(text)

			assertEquals(prefix.length, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}
	}

	private fun scan(text: String): ScanResult.Matched =
		runScanner(text, PunctuationScanner)

	companion object {
		@JvmStatic
		fun representativePunctuationProvider(): List<Arguments> = listOf(
			// 1-character
			";" to TokenKind.Semicolon,
			"{" to TokenKind.LeftBrace,
			"=" to TokenKind.Assign,

			// 2-character
			"==" to TokenKind.Equals,
			".*" to TokenKind.PtrDereference,
			".?" to TokenKind.OptionalUnwrap,
			"->" to TokenKind.SkinnyArrow,
			"+%" to TokenKind.PlusWrap,

			// 3-character
			"..." to TokenKind.Ellipsis,
			"+%=" to TokenKind.PlusWrapAssign,
			"<<|" to TokenKind.ShlSaturate,

			// 4-character
			"<<|=" to TokenKind.ShlSaturateAssign,
		).map { Arguments.of(it.first, it.second) }
	}
}