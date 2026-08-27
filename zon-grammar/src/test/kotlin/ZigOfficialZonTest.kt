package com.zigocracy.sdk.zon

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream

/**
 * Runs the official Zig ZON test suite through our lexer+parser pipeline.
 */
class ZigOfficialZonTest {

	@ParameterizedTest(name = "{0}")
	@MethodSource("zonTestFiles")
	fun `official ZON file parses successfully`(file: Path) {
		val source = Files.readString(file)
		val lexed = ZonLexer(source).tokenize()
		val tokens = when (lexed) {
			is LexResult.Success -> lexed.tokens
			is LexResult.Error -> throw AssertionError(
				"Lex failed for ${file.fileName}: ${lexed.diagnostic.message} " +
					"at line ${lexed.diagnostic.location.line},${lexed.diagnostic.location.column}"
			)
		}
		val parsed = ZonParser(tokens, source).parse()
		assertTrue(
			parsed is ParseResult.Success,
			"Parse failed for ${file.fileName}: ${(parsed as? ParseResult.Error)?.diagnostic?.message}",
		)
	}

	companion object {
		@JvmStatic
		fun zonTestFiles(): Stream<Path> {
			val dir = Path.of(
				System.getProperty("user.dir"),
				"src", "test", "resources", "zig-official-zon-test"
			)
			return Files.list(dir)
				.filter { it.toString().endsWith(".zon") }
				.sorted()
		}
	}
}
