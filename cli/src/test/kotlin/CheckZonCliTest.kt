package net.landless_city.zigocracy.cli

import com.github.ajalt.clikt.testing.test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Tests the [CheckZon] CLI command against the official Zig ZON test suite.
 */
class CheckZonCliTest {

	@Test
	fun `all official ZON test files pass when run as a directory`() {
		val testDir = "zon-grammar/src/test/resources/zig-official-zon-test"
		val cmd = CheckZon()
		val result = cmd.test(testDir)

		val hasFailures = result.output.contains('✗')
		assertEquals(false, hasFailures, "Expected no failures in output:\n${result.output}")
	}

	@Test
	fun `missing file reports error`() {
		val cmd = CheckZon()
		val result = cmd.test("nonexistent.zon")

		val hasError = result.output.contains("does not exist", ignoreCase = true)
		assertEquals(true, hasError, "Should report file error, got: ${result.output}")
	}
}
