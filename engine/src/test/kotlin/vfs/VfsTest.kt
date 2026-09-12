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
	inner class Invalidate {
		@Test
		fun `invalidates cached disk snapshot so next acquire reloads fresh disk content`(@TempDir tempDir: Path) {
			val diskFile = tempDir.resolve("main.zig")
			val text1 = "version 1"
			val text2 = "version 2"
			Files.writeString(diskFile, text1)

			val vfs = Vfs()
			val initialResult = assertInstanceOf<VfsResult.Success>(vfs.acquire(diskFile))
			assertEquals(text1, initialResult.file.text)
			assertEquals(0, initialResult.file.revision)

			Files.writeString(diskFile, text2)
			// Without invalidation, Vfs serves the cached snapshot
			assertEquals(text1, assertInstanceOf<VfsResult.Success>(vfs.acquire(diskFile)).file.text)

			val invalidated = vfs.invalidate(diskFile)
			assertTrue(invalidated)

			val refreshedResult = assertInstanceOf<VfsResult.Success>(vfs.acquire(diskFile))
			assertEquals(text2, refreshedResult.file.text)
			assertEquals(1, refreshedResult.file.revision)
		}

		@Test
		fun `does not invalidate file when active overlay exists`(@TempDir tempDir: Path) {
			val diskFile = tempDir.resolve("main.zig")
			Files.writeString(diskFile, "disk text")

			val vfs = Vfs()
			vfs.setOverlay(diskFile, "overlay text")

			val invalidated = vfs.invalidate(diskFile)
			assertFalse(invalidated)

			val result = assertInstanceOf<VfsResult.Success>(vfs.acquire(diskFile))
			assertEquals("overlay text", result.file.text)
		}

		@Test
		fun `returns false when invalidating unknown or uncached file`() {
			val vfs = Vfs()
			val unknown = Path.of("nonexistent.zig")

			assertFalse(vfs.invalidate(unknown))
		}
	}

	@Nested
	inner class Refresh {
		@Test
		fun `proactively reloads file from disk and increments revision`(@TempDir tempDir: Path) {
			val diskFile = tempDir.resolve("main.zig")
			val text1 = "first"
			val text2 = "second"
			Files.writeString(diskFile, text1)

			val vfs = Vfs()
			val res1 = assertInstanceOf<VfsResult.Success>(vfs.acquire(diskFile))
			assertEquals(text1, res1.file.text)
			assertEquals(0, res1.file.revision)

			Files.writeString(diskFile, text2)
			val refreshed = assertInstanceOf<VfsResult.Success>(vfs.refresh(diskFile))
			assertEquals(text2, refreshed.file.text)
			assertEquals(1, refreshed.file.revision)

			// Subsequent acquire should return cached refreshed snapshot
			val subsequent = assertInstanceOf<VfsResult.Success>(vfs.acquire(diskFile))
			assertSame(refreshed.file, subsequent.file)
		}

		@Test
		fun `preserves overlay when refresh is called on file with overlay`(@TempDir tempDir: Path) {
			val diskFile = tempDir.resolve("main.zig")
			Files.writeString(diskFile, "disk initial")

			val vfs = Vfs()
			val overlay = vfs.setOverlay(diskFile, "memory content")

			Files.writeString(diskFile, "disk modified")
			val refreshed = assertInstanceOf<VfsResult.Success>(vfs.refresh(diskFile))
			assertSame(overlay, refreshed.file)
			assertEquals("memory content", refreshed.file.text)
		}

		@Test
		fun `reports not found when refreshing deleted disk file`(@TempDir tempDir: Path) {
			val diskFile = tempDir.resolve("deleted.zig")
			Files.writeString(diskFile, "content")

			val vfs = Vfs()
			assertInstanceOf<VfsResult.Success>(vfs.acquire(diskFile))

			Files.delete(diskFile)
			val result = vfs.refresh(diskFile)
			assertInstanceOf<VfsResult.NotFound>(result)
		}
	}

	@Nested
	inner class Delete {
		@Test
		fun `removes overlay and active snapshot from vfs`() {
			val file = Path.of("file.zig")
			val vfs = Vfs()
			vfs.setOverlay(file, "content")

			assertTrue(vfs.hasOverlay(file))
			assertNotNull(vfs.getActiveSnapshot(file))

			val deleted = vfs.delete(file)
			assertTrue(deleted)

			assertFalse(vfs.hasOverlay(file))
			assertNull(vfs.getActiveSnapshot(file))
		}

		@Test
		fun `removes cached disk snapshot on delete`(@TempDir tempDir: Path) {
			val diskFile = tempDir.resolve("main.zig")
			Files.writeString(diskFile, "disk content")

			val vfs = Vfs()
			assertInstanceOf<VfsResult.Success>(vfs.acquire(diskFile))
			assertNotNull(vfs.getActiveSnapshot(diskFile))

			val deleted = vfs.delete(diskFile)
			assertTrue(deleted)
			assertNull(vfs.getActiveSnapshot(diskFile))
		}

		@Test
		fun `returns false when deleting untracked file`() {
			val vfs = Vfs()
			val file = Path.of("untracked.zig")

			assertFalse(vfs.delete(file))
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
