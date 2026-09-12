package com.zigocracy.sdk.engine.vfs

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class VfsTest {
	@Nested
	inner class Acquire {
		@Test
		fun `acquires file from disk successfully`(@TempDir tempDir: Path) {
			val diskFile = tempDir.resolve("main.zig")
			val text = "pub fn main() void {}"
			Files.writeString(diskFile, text)

			val vfs = Vfs()
			val result = vfs.acquire(diskFile)

			val success = assertInstanceOf<VfsResult.Success>(result)
			assertEquals(text, success.file.text)
			assertEquals(0, success.file.revision)
		}

		@Test
		fun `reports not found when file does not exist on disk`(@TempDir tempDir: Path) {
			val missing = tempDir.resolve("missing.zig")
			val vfs = Vfs()
			val result = vfs.acquire(missing)

			assertInstanceOf<VfsResult.NotFound>(result)
		}

		@Test
		fun `prefers memory overlay over disk file`(@TempDir tempDir: Path) {
			val diskFile = tempDir.resolve("main.zig")
			val diskText = "const onDisk = true;"
			val memoryText = "const inMemory = true;"
			Files.writeString(diskFile, diskText)

			val vfs = Vfs()
			vfs.setOverlay(diskFile, memoryText)

			val result = assertInstanceOf<VfsResult.Success>(vfs.acquire(diskFile))
			assertEquals(memoryText, result.file.text)
		}

		@Test
		fun `reverts to disk content when overlay is removed`(@TempDir tempDir: Path) {
			val diskFile = tempDir.resolve("main.zig")
			val diskText = "const onDisk = true;"
			val memoryText = "const inMemory = true;"
			Files.writeString(diskFile, diskText)

			val vfs = Vfs()
			vfs.setOverlay(diskFile, memoryText)
			vfs.removeOverlay(diskFile)

			val result = assertInstanceOf<VfsResult.Success>(vfs.acquire(diskFile))
			assertEquals(diskText, result.file.text)
		}
	}

	@Nested
	inner class Overlays {
		@Test
		fun `increments revision counter on subsequent overlay updates`() {
			val target = Path.of("main.zig")
			val text1 = "version 1"
			val text2 = "version 2"
			val vfs = Vfs()

			val firstRevision = vfs.setOverlay(target, text1)
			assertEquals(1, firstRevision.revision) // first edit

			val secondRevision = vfs.setOverlay(target, text2)
			assertEquals(2, secondRevision.revision) // second edit
		}

		@Test
		fun `reports whether file has active overlay`() {
			val vfs = Vfs()
			val file = Path.of("overlay_test.zig")
			val text = "pub const X = 1;"

			assertFalse(vfs.hasOverlay(file)) // clean state

			vfs.setOverlay(file, text)
			assertTrue(vfs.hasOverlay(file)) // overlay attached

			vfs.removeOverlay(file)
			assertFalse(vfs.hasOverlay(file)) // overlay detached
		}

		@Test
		fun `retrieves active snapshot by id and path`() {
			val vfs = Vfs()
			val file = Path.of("test.zig")
			val text = "test content"
			val snapshot = vfs.setOverlay(file, text)

			assertSame(snapshot, vfs.getActiveSnapshot(snapshot.id))
			assertSame(snapshot, vfs.getActiveSnapshot(file))
		}
	}

	@Nested
	inner class PathResolution {
		@Test
		fun `canonicalizes path segments lexically without symlink resolution`(@TempDir tempDir: Path) {
			val vfs = Vfs()
			val dotPath = tempDir.resolve("a/../main.zig")
			val expected = tempDir.resolve("main.zig").toAbsolutePath().normalize()

			assertEquals(expected, vfs.canonicalize(dotPath))
		}

		@Test
		fun `assigns consistent FileId for canonical paths`() {
			val vfs = Vfs()
			val p1 = Path.of("src/../src/main.zig")
			val p2 = Path.of("src/main.zig")

			val id1 = vfs.getOrRegisterId(p1)
			val id2 = vfs.getOrRegisterId(p2)

			assertEquals(id1, id2)
			assertEquals(p2.toAbsolutePath().normalize(), vfs.getPath(id1))
		}
	}
}