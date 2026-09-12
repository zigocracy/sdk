package com.zigocracy.sdk.engine.diagnostics

import com.zigocracy.sdk.engine.WorkspaceContext
import com.zigocracy.sdk.engine.vfs.VfsResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ImportAnalysisVisitorTest {

	@Nested
	inner class FileImports {
		@Test
		fun `reports no diagnostic when imported file exists on disk`(@TempDir tempDir: Path) {
			val helper = tempDir.resolve("helper.zig")
			val main = tempDir.resolve("main.zig")
			Files.writeString(helper, "pub const X = 42;")
			Files.writeString(main, "const h = @import(\"helper.zig\");")

			val ctx = WorkspaceContext.create(tempDir)
			val parsed = ctx.parse(main)
			assertNotNull(parsed)

			val diagnostics = ctx.analyze(parsed!!)
			assertTrue(diagnostics.isEmpty()) { "Expected no diagnostics for existing import, got: $diagnostics" }
		}

		@Test
		fun `reports FileNotFound when imported file does not exist`(@TempDir tempDir: Path) {
			val main = tempDir.resolve("main.zig")
			Files.writeString(main, "const missing = @import(\"nonexistent.zig\");")

			val ctx = WorkspaceContext.create(tempDir)
			val parsed = ctx.parse(main)
			assertNotNull(parsed)

			val diagnostics = ctx.analyze(parsed!!)
			assertEquals(1, diagnostics.size)
			val diag = diagnostics.first()
			assertEquals(DiagnosticCode.Import.FileNotFound, diag.code)
			assertTrue(diag.message.contains("nonexistent.zig"))
		}

		@Test
		fun `resolves file existing only in VFS overlay`(@TempDir tempDir: Path) {
			val helper = tempDir.resolve("virtual_helper.zig")
			val main = tempDir.resolve("main.zig")
			// helper does not exist on disk, only in VFS overlay
			val ctx = WorkspaceContext.create(tempDir)
			ctx.vfs.setOverlay(helper, "pub const Y = 100;")

			Files.writeString(main, "const v = @import(\"virtual_helper.zig\");")
			val parsed = ctx.parse(main)
			assertNotNull(parsed)

			val diagnostics = ctx.analyze(parsed!!)
			assertTrue(diagnostics.isEmpty()) { "Overlay file should satisfy import resolution" }
		}

		@Test
		fun `handles whitespace and comments between import tokens`(@TempDir tempDir: Path) {
			val main = tempDir.resolve("main.zig")
			Files.writeString(
				main,
				"""
				const x = @import (
					// a comment
					"missing_with_comments.zig"
				);
				""".trimIndent()
			)

			val ctx = WorkspaceContext.create(tempDir)
			val parsed = ctx.parse(main)
			assertNotNull(parsed)

			val diagnostics = ctx.analyze(parsed!!)
			assertEquals(1, diagnostics.size)
			assertEquals(DiagnosticCode.Import.FileNotFound, diagnostics.first().code)
		}
	}

	@Nested
	inner class PathValidation {
		@Test
		fun `reports InvalidPath when target contains invalid characters`(@TempDir tempDir: Path) {
			val main = tempDir.resolve("main.zig")
			Files.writeString(main, "const bad = @import(\"bad\u0000file.zig\");")

			val ctx = WorkspaceContext.create(tempDir)
			val parsed = ctx.parse(main)
			assertNotNull(parsed)

			val diagnostics = ctx.analyze(parsed!!)
			assertEquals(1, diagnostics.size)
			assertEquals(DiagnosticCode.Import.InvalidPath, diagnostics.first().code)
		}
	}

	@Nested
	inner class ModuleImports {
		@Test
		fun `does not report error for standard built-in modules`(@TempDir tempDir: Path) {
			val main = tempDir.resolve("main.zig")
			Files.writeString(
				main,
				"""
				const std = @import("std");
				const builtin = @import("builtin");
				""".trimIndent()
			)

			val ctx = WorkspaceContext.create(tempDir)
			val parsed = ctx.parse(main)
			assertNotNull(parsed)

			val diagnostics = ctx.analyze(parsed!!)
			assertTrue(diagnostics.isEmpty()) { "std and builtin should not produce false positive diagnostics" }
		}

		@Test
		fun `reports ModuleNotFound when module does not exist in module graph`(@TempDir tempDir: Path) {
			val main = tempDir.resolve("main.zig")
			Files.writeString(main, "const mypkg = @import(\"unknown_pkg\");")

			val ctx = WorkspaceContext.create(tempDir)
			// Register at least one module in the workspace so module validation is active
			ctx.moduleGraph.registerModule("my_core", tempDir.resolve("core.zig"))

			val parsed = ctx.parse(main)
			assertNotNull(parsed)

			val diagnostics = ctx.analyze(parsed!!)
			assertEquals(1, diagnostics.size)
			assertEquals(DiagnosticCode.Import.ModuleNotFound, diagnostics.first().code)
			assertTrue(diagnostics.first().message.contains("unknown_pkg"))
		}
	}
}
