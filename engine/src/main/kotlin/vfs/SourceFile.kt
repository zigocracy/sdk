package com.zigocracy.sdk.engine.vfs

import java.nio.file.Path
import kotlin.LazyThreadSafetyMode.PUBLICATION

/**
 * Immutable snapshot of a source file at a specific revision.
 */
class SourceFile(
	val id: FileId,
	val path: Path,
	val text: String,
	val revision: Int = 0,
	val originalPath: String = path.toString(),
) {
	/**
	 * Total length in UTF-16 code units.
	 */
	val width: Int = text.length

	/**
	 * Lazily initialized SHA-256 content hash.
	 */
	val contentHash: ContentHash by lazy(PUBLICATION) {
		ContentHash.compute(text)
	}

	/**
	 * Lazily initialized line map for source coordinates.
	 */
	val lineMap: LineMap by lazy(PUBLICATION) {
		LineMap.buildFor(text)
	}

	/**
	 * Returns a text slice within the file boundaries.
	 *
	 * Time Complexity: Θ(width)
	 * Memory Complexity: Θ(width)
	 */
	fun getTextSlice(start: Int, width: Int): String {
		require(start >= 0) { "Start offset must be non-negative: $start" }
		require(width >= 0) { "Width must be non-negative: $width" }
		val end = start + width
		require(end <= text.length) { "Slice bounds [$start, $end) exceed text length ${text.length}" }
		return text.substring(start, end)
	}

	companion object {
		fun forTesting(
			text: String,
			path: Path = Path.of("test.zig"),
			id: FileId = FileId(1),
			revision: Int = 0,
		): SourceFile = SourceFile(id, path, text, revision)
	}
}