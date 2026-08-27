package com.zigocracy.sdk.zig

import com.zigocracy.sdk.zig.text.LoadResult
import com.zigocracy.sdk.zig.text.SourceFile
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.io.IOException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.writeBytes

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

			assertThrows(IllegalArgumentException::class.java) {
				file.getTextSlice(start, width)
			}
		}
	}

	@Nested
	inner class DotFileHandling {
		@ParameterizedTest(name = "accepts valid dotfile path '{0}'")
		@ValueSource(strings = [".zig", "      .zig"])
		fun `accepts hidden files with correct extension before disk check`(fakePath: String, @TempDir tempDir: Path) {
			val path = tempDir.resolve(fakePath)
			val result = SourceFile.load(path)

			val error = assertInstanceOf(LoadResult.ReadError::class.java, result)
			assertInstanceOf(NoSuchFileException::class.java, error.cause)
		}
	}

	@Nested
	inner class ExtensionRejection {
		@ParameterizedTest(name = "rejects invalid extension '{0}'")
		@ValueSource(strings = ["source.txt", "Makefile", "main.ziggy", "file.zig."])
		fun `rejects unsupported file formats strictly by extension`(fakePath: String, @TempDir tempDir: Path) {
			val path = tempDir.resolve(fakePath)
			val result = SourceFile.load(path)

			val error = assertInstanceOf(LoadResult.InvalidExtension::class.java, result)
			assertEquals(path, error.path)
		}
	}

	@Nested
	inner class CaseSensitivity {
		@Test
		fun `rejects uppercase extensions strictly`(@TempDir tempDir: Path) {
			val path = tempDir.resolve("main.ZIG")
			val result = SourceFile.load(path)

			val error = assertInstanceOf(LoadResult.InvalidExtension::class.java, result)
			assertEquals("ZIG", error.extension)
		}
	}

	@Nested
	inner class ValidFileLoading {
		@ParameterizedTest(name = "loads file structure '{0}'")
		@ValueSource(strings = ["main.zig", "archive.tar.zig"])
		fun `loads file content into memory when extension format matches`(fakePath: String, @TempDir tempDir: Path) {
			val content = "const std = @import(\"std\");"
			val file = tempDir.resolve(fakePath)
			Files.writeString(file, content)

			val result = SourceFile.load(file)

			val success = assertInstanceOf(LoadResult.Success::class.java, result)
			assertEquals(content, success.file.text)
		}

		@Test
		fun `handles empty source files correctly`(@TempDir tempDir: Path) {
			val file = tempDir.resolve("empty.zig")
			Files.createFile(file)

			val result = SourceFile.load(file)

			val success = assertInstanceOf(LoadResult.Success::class.java, result)
			assertTrue(success.file.text.isEmpty())
		}
	}

	@Nested
	inner class FileSystemFaults {
		@Test
		fun `reports read error when file does not exist`(@TempDir tempDir: Path) {
			val missingFile = tempDir.resolve("missing.zig")
			val result = SourceFile.load(missingFile)

			val error = assertInstanceOf(LoadResult.ReadError::class.java, result)
			assertEquals(missingFile, error.path)
			assertInstanceOf(NoSuchFileException::class.java, error.cause)
		}

		@Test
		fun `reports read error when path targets a directory`(@TempDir tempDir: Path) {
			val directoryPath = tempDir.resolve("directory.zig")
			Files.createDirectory(directoryPath)

			val result = SourceFile.load(directoryPath)

			val error = assertInstanceOf(LoadResult.ReadError::class.java, result)
			assertEquals(directoryPath, error.path)
			assertInstanceOf(IOException::class.java, error.cause)
		}
	}

	@Nested
	inner class EncodingFaults {
		@Test
		fun `reports read error when file contains malformed UTF-8 bytes`(@TempDir tempDir: Path) {
			val file = tempDir.resolve("malformed.zig")
			val malformedBytes = byteArrayOf(0xFF.toByte(), 0xFF.toByte())
			file.writeBytes(malformedBytes)

			val result = SourceFile.load(file)

			val error = assertInstanceOf(LoadResult.ReadError::class.java, result)
			assertInstanceOf(java.nio.charset.MalformedInputException::class.java, error.cause)
		}
	}
}