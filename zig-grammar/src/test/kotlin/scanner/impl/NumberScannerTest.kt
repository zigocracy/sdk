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

class NumberScannerTest {
	@Nested
	inner class LexerCompetence {
		@Test
		fun `returns NoMatch for non digit text`() {
			val reader = StubTextReader("const")
			val result = NumberScanner.scan(reader)
			assertTrue(result is ScanResult.NoMatch)
		}
	}

	@Nested
	inner class IntegerLiterals {
		@Test
		fun `scans standard decimal integers`() {
			val result = scan("42 ")
			assertEquals(TokenKind.IntegerLiteral, result.kind)
			assertEquals(2, result.width) // "42"
		}

		@Test
		fun `greedily includes illegal digits inside integer token for recovery`() {
			// Лексер должен забрать невалидную 'G' внутрь числа ради стабильности парсера
			val result = scan("0x12G4 ")
			assertEquals(TokenKind.IntegerLiteral, result.kind)
			assertEquals(6, result.width) // "0x12G4"
		}
	}

	@Nested
	inner class FloatLiterals {
		@Test
		fun `scans classic floating point numbers`() {
			val result = scan("3.1415 ")
			assertEquals(TokenKind.FloatLiteral, result.kind)
			assertEquals(6, result.width) // "3.1415"
		}

		@Test
		fun `scans float literals terminating with a dot`() {
			val result = scan("0.")
			assertEquals(TokenKind.FloatLiteral, result.kind)
			assertEquals(2, result.width) // "0."
		}

		@Test
		fun `scans float literals terminating with a dot before trailing whitespace`() {
			val result = scan("42. \n")
			assertEquals(TokenKind.FloatLiteral, result.kind)
			assertEquals(3, result.width) // "42."
		}
	}

	@Nested
	inner class ExponentAndRadixScenarios {
		@Test
		fun `scans standard decimal exponents containing signs`() {
			val result = scan("1e-5 ")
			assertEquals(TokenKind.FloatLiteral, result.kind)
			assertEquals(4, result.width) // "1e-5"
		}

		@Test
		fun `scans hexadecimal float literals protecting internal character meanings`() {
			val result = scan("0xf.0b11 ")
			assertEquals(TokenKind.FloatLiteral, result.kind)
			assertEquals(8, result.width) // "0xf.0b11"
		}
	}

	@Nested
	inner class PunctuationBoundaries {
		@Test
		fun `isolates number width strictly before range operators`() {
			val result = scan("0..10")
			assertEquals(TokenKind.IntegerLiteral, result.kind)
			assertEquals(1, result.width) // Должен сожрать только "0", не трогая ".."
		}

		@Test
		fun `isolates number width strictly before pointer dereference and unwrap operators`() {
			val resultAsterisk = scan("42.*")
			assertEquals(TokenKind.IntegerLiteral, resultAsterisk.kind)
			assertEquals(2, resultAsterisk.width) // Только "42"

			val resultQuestion = scan("42.?")
			assertEquals(TokenKind.IntegerLiteral, resultQuestion.kind)
			assertEquals(2, resultQuestion.width) // Только "42"
		}
	}

	@Nested
	inner class RadixPrefixFailures {
		@Test
		fun `intercepts empty hexadecimal prefix during phase 1`() {
			val result = scan("0x ")
			assertEquals(TokenKind.IntegerLiteral, result.kind)
			assertEquals(2, result.width) // Забирает только "0x"

			val diagnostics = result.diagnostics
			assertEquals(1, diagnostics.size)
			assertEquals(DiagnosticCode.NumberError.MissingDigitAfterBase, diagnostics[0].code)
		}

		@Test
		fun `intercepts early exponent violation on empty hex prefix`() {
			// Вызов "0xp0" должен упасть на пустом префиксе, так как 'p' не hex-цифра
			val result = scan("0xp0")
			assertEquals(TokenKind.IntegerLiteral, result.kind)
			assertEquals(2, result.width) // Забирает только "0x"

			val diagnostics = result.diagnostics
			assertEquals(1, diagnostics.size)
			assertEquals(DiagnosticCode.NumberError.MissingDigitAfterBase, diagnostics[0].code)
		}
	}

	@Nested
	inner class NumberValidationDiagnostics {
		@Test
		fun `flags malformed trailing underscore error`() {
			val result = scan("52_ ")
			assertEquals(TokenKind.IntegerLiteral, result.kind)
			assertEquals(3, result.width) // "52_" сожрано целиком

			val errors = result.diagnostics
			assertTrue(errors.any { it.code == DiagnosticCode.NumberError.MalformedUnderscore })
		}

		@Test
		fun `flags underscores adjacent to decimal point from left`() {
			val result = scan("3_.1415")
			assertEquals(TokenKind.FloatLiteral, result.kind)
			assertEquals(7, result.width)

			val errors = result.diagnostics
			assertTrue(errors.any { it.code == DiagnosticCode.NumberError.MalformedUnderscore })
		}

		@Test
		fun `flags underscores adjacent to decimal point from right`() {
			val result = scan("3._1415")
			assertEquals(TokenKind.FloatLiteral, result.kind)
			assertEquals(7, result.width)

			val errors = result.diagnostics
			assertTrue(errors.any { it.code == DiagnosticCode.NumberError.MalformedUnderscore })
		}

		@Test
		fun `flags invalid hex letters inside body using greedy strategy`() {
			val result = scan("0x52G4 ")
			assertEquals(TokenKind.IntegerLiteral, result.kind)
			assertEquals(6, result.width) // Изолировано "0x52G4"

			val errors = result.diagnostics
			assertTrue(errors.any { it.code == DiagnosticCode.NumberError.InvalidDigit })
		}

		@Test
		fun `flags invalid trailing characters after exponent scale`() {
			// В числе 1e5x буква 'x' после экспоненты нелегальна
			val result = scan("1e5x ")
			assertEquals(TokenKind.FloatLiteral, result.kind)
			assertEquals(4, result.width) // "1e5x"

			val errors = result.diagnostics
			assertTrue(errors.any { it.code == DiagnosticCode.NumberError.InvalidDigit })
		}

		@Test
		fun `successfully passes valid complex numbers with nested underscores`() {
			val result = scan("1_000_000.00_12")
			assertEquals(TokenKind.FloatLiteral, result.kind)
			assertEquals(15, result.width)
			assertTrue(result.diagnostics.isEmpty()) // Никаких ошибок быть не должно
		}
	}

	private fun scan(text: String): ScanResult.Matched =
		runScanner(text, NumberScanner)
}