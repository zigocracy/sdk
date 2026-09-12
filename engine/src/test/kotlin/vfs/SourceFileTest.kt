package com.zigocracy.sdk.engine.vfs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class SourceFileTest {
	@Nested
	inner class FileSlicing {
		@Test
		fun `computes file width accurately in code units`() {
			val content = "const a = 1; // привет ✨"
			val file = SourceFile.forTesting(content)

			assertEquals(content.length, file.width)
		}

		@Test
		fun `extracts text slice within valid boundaries`() {
			val file = SourceFile.forTesting("const std = @import;")
			val slice = file.getTextSlice(start = 6, width = 3)

			assertEquals("std", slice)
		}

		@ParameterizedTest(name = "bounds violation: start={0}, width={1}")
		@CsvSource(
			"-1,  5",
			" 0, -5",
			"20,  5",
			"15, 10"
		)
		fun `fails to extract text slice when boundaries violate file limits`(start: Int, width: Int) {
			val file = SourceFile.forTesting("short text")

			assertThrows<IllegalArgumentException> {
				file.getTextSlice(start, width)
			}
		}
	}

	@Nested
	inner class ContentHashing {
		@Test
		fun `computes hash`() {
			val hash = ContentHash.compute("test content")
			assertEquals(64, hash.hex.length)
		}

		@Test
		fun `computes SHA-256 hex digest`() {
			val file1 = SourceFile.forTesting("const x = 42;")
			val file2 = SourceFile.forTesting("const x = 42;")
			val file3 = SourceFile.forTesting("const x = 43;")

			assertEquals(file1.contentHash, file2.contentHash)
			assertNotEquals(file1.contentHash, file3.contentHash)
		}
	}
}