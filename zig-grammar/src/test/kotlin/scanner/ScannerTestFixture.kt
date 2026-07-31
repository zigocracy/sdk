package net.landless_city.zigocracy.zig.scanner

import net.landless_city.zigocracy.zig.text.impl.StringTextStream
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Executes a [scanner] against the input [text] and asserts that it returns a successful match.
 *
 * @throws AssertionError If the scanner rejects the text and returns [ScanResult.NoMatch].
 */
internal fun runScanner(text: String, scanner: Scanner): ScanResult.Matched {
	val reader = StringTextStream(text)
	val result = scanner.scan(reader)

	assertTrue(result is ScanResult.Matched) {
		"Expected ScanResult.Matched but found ${result::class.simpleName} instead."
	}
	return result as ScanResult.Matched
}