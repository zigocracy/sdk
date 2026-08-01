package net.landless_city.zigocracy.zig.scanner.impl

import net.landless_city.zigocracy.zig.scanner.ScanResult
import net.landless_city.zigocracy.zig.scanner.StubTextReader
import net.landless_city.zigocracy.zig.scanner.runScanner
import net.landless_city.zigocracy.zig.syntax.TokenKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BuiltinIdentiiferScannerTest {
	@Nested
	inner class LexerCompetence {
		@Test
		fun `returns NoMatch if text does not start with at symbol`() {
			val reader = StubTextReader("import")
			val result = BuiltinIdentiiferScanner.scan(reader)

			assertTrue(result is ScanResult.NoMatch) { "Builtin scanner must reject text without '@'." }
		}

		@Test
		fun `returns NoMatch and yields to identifier scanner when encountering quoted name`() {
			val reader = StubTextReader("@\"while\"")
			val result = BuiltinIdentiiferScanner.scan(reader)

			assertTrue(result is ScanResult.NoMatch) { "Must return NoMatch on quoted identifiers to let IdentifierScanner parse them." }
		}

		@Test
		fun `returns NoMatch when at symbol is followed by numbers or spaces`() {
			assertTrue(BuiltinIdentiiferScanner.scan(StubTextReader("@123")) is ScanResult.NoMatch)
			assertTrue(BuiltinIdentiiferScanner.scan(StubTextReader("@ ")) is ScanResult.NoMatch)
		}
	}

	@Nested
	inner class HappyPath {
		@Test
		fun `matches standard builtin functions`() {
			val resultImport = scan("@import")
			assertEquals(TokenKind.BuiltinIdentifier, resultImport.kind)
			assertEquals(7, resultImport.width)

			val resultMemcpy = scan("@memcpy")
			assertEquals(TokenKind.BuiltinIdentifier, resultMemcpy.kind)
			assertEquals(7, resultMemcpy.width)
		}

		@Test
		fun `matches builtin identifier containing underscores and numbers`() {
			val result = scan("@typeInfo_2")
			assertEquals(TokenKind.BuiltinIdentifier, result.kind)
			assertEquals(11, result.width)
		}
	}

	private fun scan(text: String): ScanResult.Matched =
		runScanner(text, BuiltinIdentiiferScanner)
}