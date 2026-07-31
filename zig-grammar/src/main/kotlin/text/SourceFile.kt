package net.landless_city.zigocracy.zig.text

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.extension

class SourceFile private constructor(
	val path: Path,
	val text: String,
) {
	/**
	 * The total length of the file content in UTF-16 code units.
	 */
	val width: CodeUnits = text.length

	/**
	 * Extracts a substring from the source code based on starting position and length.
	 */
	fun getTextSlice(start: CodeUnits, width: CodeUnits): String {
		require(start >= 0 && width >= 0 && start + width <= text.length) {
			"Invalid slice boundaries. Got start: $start, width: $width, but file max width is ${text.length}."
		}
		return text.substring(start, start + width)
	}

	companion object {
		fun load(path: Path): LoadResult {
			if (path.extension != "zig") {
				return LoadResult.InvalidExtension(path, path.extension)
			}

			try {
				val canonicalPath = path.toAbsolutePath().normalize()
				val text = Files.readString(canonicalPath)

				val sourceFile = SourceFile(canonicalPath, text)
				return LoadResult.Success(sourceFile)
			} catch (e: IOException) {
				return LoadResult.ReadError(path, e)
			}
		}

		/**
		 * Convenience factory for testing. Creates an in-memory [SourceFile]
		 * without touching the physical disk or checking extensions.
		 */
		fun forTesting(content: String, fakePath: String = "test.zig"): SourceFile {
			return SourceFile(Path(fakePath), content)
		}
	}
}

sealed interface LoadResult {
	data class Success(val file: SourceFile) : LoadResult
	data class InvalidExtension(val path: Path, val extension: String) : LoadResult
	data class ReadError(val path: Path, val cause: Throwable) : LoadResult
}