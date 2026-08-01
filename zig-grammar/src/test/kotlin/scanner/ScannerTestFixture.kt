package net.landless_city.zigocracy.zig.scanner

import net.landless_city.zigocracy.zig.shared.CodeUnits
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * An in-memory text provider for isolating scanner behaviors during unit tests.
 * This structure is entirely stateless and safe for concurrent parallel test threads.
 */
class StubTextReader(private val text: String) : TextReader {

	override fun peekChar(offset: CodeUnits): Char? = text.getOrNull(offset)

	override fun peekString(width: CodeUnits, offset: CodeUnits): String {
		if (offset >= text.length || offset < 0 || width <= 0) return ""
		return text.drop(offset).take(width)
	}
}

/**
 * Helper utility ensuring that the evaluated [scanner] commits to a successful match.
 *
 * Examples of expected outcomes:
 * - `runScanner("42", NumberScanner)` -> Automatically asserts type and returns ScanResult.Matched payload.
 * - `runScanner("abc", NumberScanner)` -> Fails the test immediately with an explicit assertion layout message.
 */
fun runScanner(text: String, scanner: TokenScanner): ScanResult.Matched {
	val reader = StubTextReader(text)
	val result = scanner.scan(reader)

	// Fast-fail assertion
	assertTrue(result is ScanResult.Matched) {
		"Expected ScanResult.Matched, but encountered: ${result::class.simpleName}"
	}
	return result as ScanResult.Matched
}