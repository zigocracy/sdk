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

class BuiltinIdentifierScannerTest {
	@Nested
	inner class LexerCompetence {
		@Test
		fun `returns NoMatch if text does not start with at symbol`() {
			val reader = StringTextStream("import")
			val result = BuiltinIdentifierScanner.scan(reader)

			assertTrue(result is ScanResult.NoMatch) { "Builtin scanner must reject text without '@'." }
		}

		@Test
		fun `returns NoMatch and yields to identifier scanner when encountering quoted name`() {
			val reader = StringTextStream("@\"while\"")
			val result = BuiltinIdentifierScanner.scan(reader)

			assertTrue(result is ScanResult.NoMatch) { "Must return NoMatch on quoted identifiers to let IdentifierScanner parse them." }
		}

		@ParameterizedTest(name = "returns NoMatch when at symbol is followed by invalid suffix '{0}'")
		@ValueSource(strings = ["123", " ", "\n", ""])
		fun `returns NoMatch when at symbol is followed by numbers or spaces`(suffix: String) {
			val text = "@$suffix"
			val reader = StringTextStream(text)
			val result = BuiltinIdentifierScanner.scan(reader)

			assertTrue(result is ScanResult.NoMatch) { "Builtin scanner must reject invalid suffix '$suffix' after '@'." }
		}
	}

	@Nested
	inner class HappyPath {
		@ParameterizedTest(name = "matches standard builtin function '{0}'")
		@ValueSource(strings = ["@import", "@sin", "@typeInfo", "@TypeOf"])
		fun `matches standard builtin functions`(text: String) {
			val result = scan(text)

			assertEquals(TokenKind.BuiltinIdentifier, result.kind)
			assertEquals(text.length, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}

		@ParameterizedTest(name = "matches complex builtin identifier '{0}' containing underscores and numbers")
		@ValueSource(strings = ["@typeInfo_2", "@panic_123", "@_builtin"])
		fun `matches builtin identifier containing underscores and numbers`(text: String) {
			val result = scan(text)

			assertEquals(TokenKind.BuiltinIdentifier, result.kind)
			assertEquals(text.length, result.width)
			assertTrue(result.diagnostics.isEmpty())
		}
	}

	private fun scan(text: String): ScanResult.Matched =
		runScanner(text, BuiltinIdentifierScanner)
}