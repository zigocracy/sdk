package net.landless_city.zigocracy.zig

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.writeBytes

private const val zigCode = "const std = @import(\"std\");"

class SourceFileTest {
	@Nested
	inner class SuccessResult {

		@Test
		fun `loads file content when extension is correct`(@TempDir tempDir: Path) {
			val file = tempDir.resolve("main.zig")
			Files.writeString(file, zigCode)

			val result = SourceFile.load(file)

			val success = assertInstanceOf(LoadResult.Success::class.java, result)
			assertEquals(file, success.file.path)
			assertEquals(zigCode, success.file.text)
		}

		@Test
		fun `loads hidden dotfiles when extension is correct`(@TempDir tempDir: Path) {
			val file = tempDir.resolve(".zig")
			Files.writeString(file, zigCode)

			val result = SourceFile.load(file)

			val success = assertInstanceOf(LoadResult.Success::class.java, result)
			assertEquals(zigCode, success.file.text)
		}

		@Test
		fun `loads files even if name is blank before extension`(@TempDir tempDir: Path) {
			val file = tempDir.resolve("      .zig")
			Files.writeString(file, zigCode)

			val result = SourceFile.load(file)

			val success = assertInstanceOf(LoadResult.Success::class.java, result)
			assertEquals(zigCode, success.file.text)
		}

		@Test
		fun `loads files with multiple dots when final extension is correct`(@TempDir tempDir: Path) {
			val file = tempDir.resolve("archive.tar.zig")
			Files.writeString(file, zigCode)

			val result = SourceFile.load(file)

			val success = assertInstanceOf(LoadResult.Success::class.java, result)
			assertEquals(zigCode, success.file.text)
		}

		@Test
		fun `handles empty files correctly`(@TempDir tempDir: Path) {
			val file = tempDir.resolve("empty.zig")
			Files.createFile(file)

			val result = SourceFile.load(file)

			val success = assertInstanceOf(LoadResult.Success::class.java, result)
			assertEquals("", success.file.text)
		}
	}

	@Nested
	inner class InvalidExtensionResult {

		@Test
		fun `fails when file has wrong extension`(@TempDir tempDir: Path) {
			val file = tempDir.resolve("source.txt")
			Files.createFile(file)

			val result = SourceFile.load(file)

			val error = assertInstanceOf(LoadResult.InvalidExtension::class.java, result)
			assertEquals(file, error.path)
			assertEquals("txt", error.extension)
		}

		@Test
		fun `fails when file has no extension`(@TempDir tempDir: Path) {
			val file = tempDir.resolve("Makefile")
			Files.createFile(file)

			val result = SourceFile.load(file)

			val error = assertInstanceOf(LoadResult.InvalidExtension::class.java, result)
			assertEquals("", error.extension)
		}

		@Test
		fun `fails when extension is part of a longer word`(@TempDir tempDir: Path) {
			val file = tempDir.resolve("main.ziggy")
			Files.createFile(file)

			val result = SourceFile.load(file)

			val error = assertInstanceOf(LoadResult.InvalidExtension::class.java, result)
			assertEquals(file, error.path)
			assertEquals("ziggy", error.extension)
		}

		@Test
		fun `fails when file ends with a dot`(@TempDir tempDir: Path) {
			val file = tempDir.resolve("file.zig.")
			Files.createFile(file)

			val result = SourceFile.load(file)

			val error = assertInstanceOf(LoadResult.InvalidExtension::class.java, result)
			assertEquals(file, error.path)
			assertEquals("", error.extension)
		}

		@Test
		fun `fails when extension is uppercase`(@TempDir tempDir: Path) {
			val file = tempDir.resolve("main.ZIG")
			Files.createFile(file)

			val result = SourceFile.load(file)

			val error = assertInstanceOf(LoadResult.InvalidExtension::class.java, result)
			assertEquals(file, error.path)
			assertEquals("ZIG", error.extension)
		}
	}

	@Nested
	inner class ReadErrorResult {

		@Test
		fun `fails when file does not exist`() {
			val missingFile = Path.of("missing.zig")

			val result = SourceFile.load(missingFile)

			val error = assertInstanceOf(LoadResult.ReadError::class.java, result)
			assertEquals(missingFile, error.path)
			assertInstanceOf(NoSuchFileException::class.java, error.cause)
		}

		@Test
		fun `fails when path is a directory instead of a file`(@TempDir tempDir: Path) {
			val directoryPath = tempDir.resolve("directory.zig")
			directoryPath.createDirectory()

			val result = SourceFile.load(directoryPath)

			val error = assertInstanceOf(LoadResult.ReadError::class.java, result)
			assertEquals(directoryPath, error.path)
			assertInstanceOf(IOException::class.java, error.cause)
		}
	}

	@Test
	fun `fails when file contains malformed UTF-8 bytes`(@TempDir tempDir: Path) {
		val file = tempDir.resolve("malformed.zig")
		val malformedBytes = byteArrayOf(0xFF.toByte(), 0xFF.toByte())
		file.writeBytes(malformedBytes)

		val result = SourceFile.load(file)

		val error = assertInstanceOf(LoadResult.ReadError::class.java, result)
		assertInstanceOf(java.nio.charset.MalformedInputException::class.java, error.cause)
	}
}
