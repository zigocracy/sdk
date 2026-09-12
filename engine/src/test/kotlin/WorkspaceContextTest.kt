package com.zigocracy.sdk.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import java.nio.file.Path

class WorkspaceContextTest {
	@Test
	fun `creates workspace context with normalized root path`() {
		val relativePath = Path.of("foo/../bar")
		val ctxWithRoot = WorkspaceContext.create(relativePath)

		assertEquals(Path.of("bar").toAbsolutePath().normalize(), ctxWithRoot.rootPath)
	}

	@Test
	fun `creates workspace context with null root path when path is missing`() {
		val ctxWithoutRoot = WorkspaceContext.create(null)

		assertNull(ctxWithoutRoot.rootPath)
	}

	@Test
	fun `parse returns null when file is missing in vfs`() {
		val ctx = WorkspaceContext.create()
		val path = Path.of("app.zig")

		assertNull(ctx.parse(path))
	}

	@Test
	fun `parse acquires file via vfs and parses when present`() {
		val ctx = WorkspaceContext.create()
		val path = Path.of("app.zig")
		ctx.vfs.setOverlay(path, "const overlay = true;")

		val parsed = ctx.parse(path)

		assertNotNull(parsed)
		assertEquals("const overlay = true;", parsed.source.text)
	}
}