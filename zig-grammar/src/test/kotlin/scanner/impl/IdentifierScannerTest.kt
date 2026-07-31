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

class IdentifierScannerTest {
	@Nested
	inner class LexerCompetence {
		@Test
		fun `returns NoMatch for punctuation symbols`() {
			val reader = StubTextReader("==")
			val result = IdentifierScanner.scan(reader)
			assertTrue(result is ScanResult.NoMatch)
		}

		@Test
		fun `returns NoMatch for spaces`() {
			val reader = StubTextReader("  ")
			val result = IdentifierScanner.scan(reader)
			assertTrue(result is ScanResult.NoMatch)
		}
	}

	@Nested
	inner class KeywordsMatching {
		@Test
		fun `matches basic short keywords`() {
			val resultFn = scan("fn")
			assertEquals(TokenKind.FnKeyword, resultFn.kind)
			assertEquals(2, resultFn.width)

			val resultIf = scan("if")
			assertEquals(TokenKind.IfKeyword, resultIf.kind)
			assertEquals(2, resultIf.width)
		}

		@Test
		fun `matches complex long keywords`() {
			val result = scan("threadlocal")
			assertEquals(TokenKind.ThreadlocalKeyword, result.kind)
			assertEquals(11, result.width)
		}

		@Test
		fun `treats true and false as plain identifiers`() {
			val resultTrue = scan("true")
			assertEquals(TokenKind.Identifier, resultTrue.kind)
			assertEquals(4, resultTrue.width)

			val resultFalse = scan("false")
			assertEquals(TokenKind.Identifier, resultFalse.kind)
			assertEquals(5, resultFalse.width)
		}
	}

	@Nested
	inner class PlainUserIdentifiers {
		@Test
		fun `matches standard ascii names with underscores and numbers`() {
			val result = scan("my_variable_42")
			assertEquals(TokenKind.Identifier, result.kind)
			assertEquals(14, result.width)
		}

		@Test
		fun `stops before punctuation symbol without overconsuming`() {
			val result = scan("foo+bar")
			assertEquals(TokenKind.Identifier, result.kind)
			assertEquals(3, result.width) // Должен считать только "foo"
		}
	}

	@Nested
	inner class UnicodeUts55Compliance {
		@Test
		fun `matches international alphabetic names`() {
			// Проверяем русские буквы
			val result = scan("HTTPОтвет")
			assertEquals(TokenKind.Identifier, result.kind)
			assertEquals(9, result.width)
		}

		@Test
		fun `safely matches 32-bit supplementary mathematical symbols`() {
			// Символ '𝚪' (Gamma) — кодовая точка 0x1D6AA, занимает 2 Char в UTF-16 (суррогатная пара)
			val text = "my𝚪Var"
			val result = scan(text)

			assertEquals(TokenKind.Identifier, result.kind)
			// "my" (2) + '𝚪' (2 кодовые единицы) + "Var" (3) = 7 кодовых единиц (width)
			assertEquals(7, result.width) { "Should correctly track surrogate pair physical width." }
		}

		@Test
		fun `safely matches 32-bit CJK extension ideographs`() {
			// Символ '𠮷' — кодовая точка 0x20BB7, занимает 2 Char в UTF-16
			val text = "𠮷_lucky"
			val result = scan(text)

			assertEquals(TokenKind.Identifier, result.kind)
			// '𠮷' (2 кодовые единицы) + "_" (1) + "lucky" (5) = 8 кодовых единиц (width)
			assertEquals(8, result.width)
		}
	}

	@Nested
	inner class EscapedIdentifiers {
		@Test
		fun `matches simple perfect escaped identifier`() {
			// @"while" -> 1 (@) + 7 (строка "while") = 8 кодовых единиц
			val result = scan("@\"while\"")
			assertEquals(TokenKind.Identifier, result.kind)
			assertEquals(8, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}

		@Test
		fun `matches escaped identifier containing valid hex escape sequences`() {
			// @"foo\x42bar" -> 1 (@) + 12 (строка "foo\x42bar") = 13 кодовых единиц
			val result = scan(
				"""
	@"foo\x42bar"
	""".trimIndent()
			)
			assertEquals(TokenKind.Identifier, result.kind)
			assertEquals(13, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}

		@Test
		fun `handles unterminated escaped identifier due to raw newline`() {
			// @"hello\n -> 1 (@) + 6 (незакрытая строка "hello) = 7 кодовых единиц. \n остается снаружи
			val text = "@\"hello\n"
			val result = scan(text)

			assertEquals(TokenKind.Identifier, result.kind)
			assertEquals(7, result.width)

			val diagnostics = result.diagnostics
			assertEquals(1, diagnostics.size)

			val error = diagnostics[0]
			assertEquals(DiagnosticCode.StringError.UnterminatedString, error.code)
			assertEquals(7, error.relativeStart)
			assertEquals(0, error.width)
		}
	}

	@Nested
	inner class BlankIdentifierMatching {
		@Test
		fun `matches lone underscore as a blank identifier`() {
			val result = scan("_")
			assertEquals(TokenKind.BlankIdentifier, result.kind)
			assertEquals(1, result.width)
		}

		@Test
		fun `matches multiple underscores as a normal identifier`() {
			val result = scan("__")
			assertEquals(TokenKind.Identifier, result.kind)
			assertEquals(2, result.width)
		}

		@Test
		fun `matches text with trailing underscore as a normal identifier`() {
			val result = scan("_foo")
			assertEquals(TokenKind.Identifier, result.kind)
			assertEquals(4, result.width)
		}

		@Test
		fun `matches text with leading underscore as a normal identifier`() {
			val result = scan("foo_")
			assertEquals(TokenKind.Identifier, result.kind)
			assertEquals(4, result.width)
		}
	}

	private fun scan(text: String): ScanResult.Matched =
		runScanner(text, IdentifierScanner)
}