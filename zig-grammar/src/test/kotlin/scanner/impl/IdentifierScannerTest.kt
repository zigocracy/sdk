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
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.stream.Stream

class IdentifierScannerTest {

	@Nested
	inner class LexerCompetence {
		@ParameterizedTest(name = "returns NoMatch for non-identifier input '{0}'")
		@ValueSource(strings = ["==", "  ", "\n", "++"])
		fun `returns NoMatch for non-identifier inputs`(text: String) {
			val reader = StringTextStream(text)
			val result = IdentifierScanner.scan(reader)

			assertTrue(result is ScanResult.NoMatch) { "Identifier scanner must reject non-identifier input." }
		}
	}

	@Nested
	inner class HappyPath {
		@ParameterizedTest(name = "matches keyword '{0}' with correct TokenKind")
		@MethodSource("com.zigocracy.sdk.zig.scanner.impl.IdentifierScannerTest#keywordProvider")
		fun `matches keywords correctly`(keyword: String, expectedKind: TokenKind) {
			val result = scan(keyword)

			assertEquals(expectedKind, result.kind)
			assertEquals(keyword.length, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}

		@ParameterizedTest(name = "treats builtin literal '{0}' as a plain identifier")
		@ValueSource(strings = ["true", "false", "null", "undefined"])
		fun `treats builtin literals as plain identifiers`(text: String) {
			val result = scan(text)

			assertEquals(TokenKind.Identifier, result.kind)
			assertEquals(text.length, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}

		@ParameterizedTest(name = "matches standard ascii name '{0}'")
		@ValueSource(strings = ["my_variable_42", "foo", "camelCaseName", "A123_b456"])
		fun `matches standard ascii names with underscores and numbers`(text: String) {
			val result = scan(text)

			assertEquals(TokenKind.Identifier, result.kind)
			assertEquals(text.length, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}

		@ParameterizedTest(name = "matches Unicode name '{0}'")
		@ValueSource(strings = ["HTTPОтвет", "переменная", "测试_variable", "my𝚪Var", "𠮷_lucky"])
		fun `matches international alphabetic names under UTS55`(text: String) {
			val result = scan(text)

			assertEquals(TokenKind.Identifier, result.kind)
			assertEquals(text.length, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}

		@ParameterizedTest(name = "matches valid escaped identifier '{0}'")
		@ValueSource(strings = ["@\"while\"", "@\"foo\\x42bar\"", "@\"variable name with spaces\""])
		fun `matches valid escaped identifiers`(text: String) {
			val result = scan(text)

			assertEquals(TokenKind.Identifier, result.kind)
			assertEquals(text.length, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}

		@Test
		fun `matches lone underscore as a blank identifier`() {
			val text = "_"
			val result = scan(text)

			assertEquals(TokenKind.BlankIdentifier, result.kind)
			assertEquals(text.length, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}

		@ParameterizedTest(name = "matches non-blank underscore pattern '{0}' as normal identifier")
		@ValueSource(strings = ["__", "_foo", "foo_", "foo_bar"])
		fun `matches text with underscores as a normal identifier`(text: String) {
			val result = scan(text)

			assertEquals(TokenKind.Identifier, result.kind)
			assertEquals(text.length, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}
	}

	@Nested
	inner class LineEndBoundaries {
		@ParameterizedTest(name = "stops consumption exactly before newline #{index}")
		@ValueSource(strings = ["\n", "\r\n", "\r"])
		fun `stops consumption exactly before line endings`(lineEnding: String) {
			val prefix = "@\"hello"
			val text = "$prefix$lineEnding"
			val result = scan(text)

			assertEquals(TokenKind.Identifier, result.kind)
			assertEquals(prefix.length, result.width)

			val diagnostics = result.diagnostics
			assertEquals(1, diagnostics.size)

			val error = diagnostics[0]
			assertEquals(DiagnosticCode.StringError.UnterminatedString, error.code)
			assertEquals(prefix.length, error.offset)
			assertEquals(0, error.width)
		}

		@Test
		fun `stops before punctuation symbol without overconsuming`() {
			val prefix = "foo"
			val text = "$prefix+bar"
			val result = scan(text)

			assertEquals(TokenKind.Identifier, result.kind)
			assertEquals(prefix.length, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}
	}

	private fun scan(text: String): ScanResult.Matched =
		runScanner(text, IdentifierScanner)

	companion object {
		@JvmStatic
		fun keywordProvider(): Stream<Arguments> = Stream.of(
			Arguments.of("fn", TokenKind.FnKeyword),
			Arguments.of("if", TokenKind.IfKeyword),
			Arguments.of("threadlocal", TokenKind.ThreadlocalKeyword),
		)
	}
}