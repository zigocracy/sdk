package com.zigocracy.sdk.engine

import com.zigocracy.sdk.engine.vfs.SourceFile
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull

class ParseCacheTest {
	@Test
	fun `parses syntax stream and caches by content hash`() {
		val cache = ParseCache()
		val file1 = SourceFile.forTesting("pub fn main() void {}")
		val file2 = SourceFile.forTesting("pub fn main() void {}")

		val stream1 = cache.getOrParse(file1)
		val stream2 = cache.getOrParse(file2)

		assertNotNull(stream1)
		assertSame(stream1, stream2)
	}

	@Test
	fun `parses separate syntax stream when content differs`() {
		val cache = ParseCache()
		val file1 = SourceFile.forTesting("pub fn a() void {}")
		val file2 = SourceFile.forTesting("pub fn b() void {}")

		val stream1 = cache.getOrParse(file1)
		val stream2 = cache.getOrParse(file2)

		assertNotSame(stream1, stream2)
	}

	@Test
	fun `re-parses syntax after invalidation`() {
		val cache = ParseCache()
		val file = SourceFile.forTesting("const x = 1;")

		val stream1 = cache.getOrParse(file)
		cache.invalidate(file.contentHash)
		val stream2 = cache.getOrParse(file)

		assertNotSame(stream1, stream2)
	}

	@Test
	fun `clears all cached syntax streams`() {
		val cache = ParseCache()
		val file1 = SourceFile.forTesting("const x = 1;")
		val file2 = SourceFile.forTesting("const y = 2;")

		val stream1 = cache.getOrParse(file1)
		val stream2 = cache.getOrParse(file2)

		cache.clear()

		val newStream1 = cache.getOrParse(file1)
		val newStream2 = cache.getOrParse(file2)

		assertNotSame(stream1, newStream1)
		assertNotSame(stream2, newStream2)
	}
}