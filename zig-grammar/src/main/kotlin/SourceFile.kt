package net.landless_city.zigocracy.zig

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension

class SourceFile private constructor(
	val path: Path,
	val text: String,
) {
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
	}
}

sealed interface LoadResult {
	data class Success(val file: SourceFile) : LoadResult
	data class InvalidExtension(val path: Path, val extension: String) : LoadResult
	data class ReadError(val path: Path, val cause: Throwable) : LoadResult
}