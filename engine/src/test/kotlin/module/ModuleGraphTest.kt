package com.zigocracy.sdk.engine.module

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ModuleGraphTest {
	@Nested
	inner class Classification {
		@Test
		fun `classifies zig extension as Zig source file import`() {
			val kind = ImportKind.classify("utils/math.zig")

			val fileKind = assertInstanceOf<ImportKind.File>(kind)
			assertEquals(Path.of("utils/math.zig"), fileKind.path)
			assertEquals(FileKind.Zig, fileKind.kind)
		}

		@Test
		fun `classifies zon extension as ZON data file import`() {
			val kind = ImportKind.classify("build.zig.zon")

			val fileKind = assertInstanceOf<ImportKind.File>(kind)
			assertEquals(Path.of("build.zig.zon"), fileKind.path)
			assertEquals(FileKind.Zon, fileKind.kind)
		}

		@Test
		fun `classifies capitalized extension as named module import`() {
			val kind = ImportKind.classify("example.Zig")

			val moduleKind = assertInstanceOf<ImportKind.Module>(kind)
			assertEquals("example.Zig", moduleKind.name)
		}

		@Test
		fun `classifies standard target as named module import`() {
			val kind = ImportKind.classify("std")

			val moduleKind = assertInstanceOf<ImportKind.Module>(kind)
			assertEquals("std", moduleKind.name)
		}

		@Test
		fun `classifies path with invalid filesystem characters as invalid path`() {
			val kind = ImportKind.classify("bad\u0000file.zig")

			val invalid = assertInstanceOf<ImportKind.InvalidFilePath>(kind)
			assertEquals("bad\u0000file.zig", invalid.rawTarget)
			assertEquals(FileKind.Zig, invalid.kind)
		}
	}

	@Nested
	inner class Registration {
		@Test
		fun `registers module named main by default`() {
			val graph = ModuleGraph()
			val targetFile = Path.of("src/script.zig")

			val module = graph.getOrRegisterModule(targetFile)

			assertEquals("main", module.name)
			assertEquals(targetFile.toAbsolutePath().normalize(), module.rootFile)
		}

		@Test
		fun `uses parent directory as module root directory`() {
			val graph = ModuleGraph()
			val file = Path.of("src/app.zig")
			val expectedDir = file.toAbsolutePath().normalize().parent

			val module = graph.getOrRegisterModule(file)

			assertEquals(expectedDir, module.rootDirectory)
		}

		@Test
		fun `returns existing module when already registered for root file`() {
			val graph = ModuleGraph()
			val mainFile = Path.of("src/main.zig")
			val registered = graph.registerModule("custom", mainFile)

			val module = graph.getOrRegisterModule(mainFile)

			assertSame(registered, module)
			assertEquals("custom", module.name)
		}

		@Test
		fun `looks up module by id, name, and root file`() {
			val graph = ModuleGraph()
			val file = Path.of("libs/core/core.zig")
			val registered = graph.registerModule("core", file)

			assertSame(registered, graph.getModule(registered.id))
			assertSame(registered, graph.findModuleByName("core"))
			assertSame(registered, graph.findModuleByRootFile(file))
			assertNull(graph.findModuleByName("non_existent"))
			assertNull(graph.findModuleByRootFile(Path.of("other.zig")))
		}

		@Test
		fun `returns all registered modules`() {
			val graph = ModuleGraph()
			val mod1 = graph.registerModule("mod1", Path.of("a/root.zig"))
			val mod2 = graph.registerModule("mod2", Path.of("b/root.zig"))

			val all = graph.getAllModules()
			assertEquals(2, all.size)
			assertTrue(all.contains(mod1))
			assertTrue(all.contains(mod2))
		}
	}

	@Nested
	inner class Dependencies {
		@Test
		fun `merges implicit dependencies into registered module`() {
			val graph = ModuleGraph()
			val std = graph.registerModule("std", Path.of("std/std.zig"))

			val app = graph.registerModule(
				name = "app",
				rootFile = Path.of("src/main.zig"),
				implicitDependencies = mapOf("std" to std.id),
			)

			assertEquals(std.id, app.dependencies["std"])
		}

		@Test
		fun `explicit dependencies override implicit dependencies`() {
			val graph = ModuleGraph()
			val defaultStd = graph.registerModule("std", Path.of("std/default.zig"))
			val customStd = graph.registerModule("my_std", Path.of("my_std/lib.zig"))

			val app = graph.registerModule(
				name = "app",
				rootFile = Path.of("src/main.zig"),
				dependencies = mapOf("std" to customStd.id),
				implicitDependencies = mapOf("std" to defaultStd.id),
			)

			assertEquals(customStd.id, app.dependencies["std"])
		}
	}

	@Nested
	inner class Resolution {
		@Test
		fun `resolves relative file import within same directory`() {
			val graph = ModuleGraph()
			val fromFile = Path.of("src/main.zig").toAbsolutePath().normalize()
			val module = graph.getOrRegisterModule(fromFile)

			val resolution = graph.resolve(
				currentModule = module,
				currentFile = fromFile,
				importTarget = "utils/math.zig",
			)

			val fileRes = assertInstanceOf<ImportResolution.File>(resolution)
			val expected = fromFile.parent.resolve("utils/math.zig").normalize()
			assertEquals(expected, fileRes.targetPath)
			assertEquals(FileKind.Zig, fileRes.kind)
		}

		@Test
		fun `resolves relative file import traversing up to parent directory`() {
			val graph = ModuleGraph()
			val rootDir = Path.of("src").toAbsolutePath().normalize()
			val fromFile = rootDir.resolve("sub/worker.zig")
			val module = graph.registerModule(name = "src", rootFile = fromFile, rootDirectory = rootDir)

			val resolution = graph.resolve(
				currentModule = module,
				currentFile = fromFile,
				importTarget = "../shared.zig",
			)

			val fileRes = assertInstanceOf<ImportResolution.File>(resolution)
			val expected = rootDir.resolve("shared.zig").normalize()
			assertEquals(expected, fileRes.targetPath)
		}

		@Test
		fun `reports file outside module when relative import escapes module root directory`() {
			val graph = ModuleGraph()
			val rootDir = Path.of("project", "my_pkg").toAbsolutePath().normalize()
			val fromFile = rootDir.resolve("src").resolve("main.zig")
			val module = graph.registerModule(name = "my_pkg", rootFile = fromFile, rootDirectory = rootDir)

			val resolution = graph.resolve(
				currentModule = module,
				currentFile = fromFile,
				importTarget = "../../secret.zig",
			)

			val outside = assertInstanceOf<ImportResolution.FileOutsideModule>(resolution)
			val expectedPath = rootDir.parent.resolve("secret.zig").normalize()
			assertEquals(expectedPath, outside.targetPath)
			assertEquals(rootDir, outside.moduleRootDirectory)
			assertEquals(FileKind.Zig, outside.kind)
		}

		@Test
		fun `prefers module import over local file with same name`(@TempDir tempDir: Path) {
			val graph = ModuleGraph()
			val modFile = tempDir.resolve("lib.zig")
			val importedMod = graph.registerModule("example.zig", modFile)

			val fromFile = tempDir.resolve("main.zig")
			val collidingLocalFile = tempDir.resolve("example.zig")
			Files.writeString(collidingLocalFile, "pub const X = 1;")

			val callingModule = graph.registerModule(
				name = "caller",
				rootFile = fromFile,
				dependencies = mapOf("example.zig" to importedMod.id),
			)

			val resolution = graph.resolve(
				currentModule = callingModule,
				currentFile = fromFile,
				importTarget = "example.zig",
			)

			val modRes = assertInstanceOf<ImportResolution.ModuleTarget>(resolution)
			assertEquals(importedMod.id, modRes.module.id)
			assertEquals(collidingLocalFile.toAbsolutePath().normalize(), modRes.shadowedFilePath)
		}

		@Test
		fun `resolves module import without local file collision`() {
			val graph = ModuleGraph()
			val modFile = Path.of("libs/network/network.zig").toAbsolutePath().normalize()
			val netMod = graph.registerModule("network", modFile)

			val fromFile = Path.of("src/main.zig").toAbsolutePath().normalize()
			val callingModule = graph.registerModule(
				name = "caller",
				rootFile = fromFile,
				dependencies = mapOf("network" to netMod.id),
			)

			val resolution = graph.resolve(
				currentModule = callingModule,
				currentFile = fromFile,
				importTarget = "network",
			)

			val modRes = assertInstanceOf<ImportResolution.ModuleTarget>(resolution)
			assertEquals(netMod.id, modRes.module.id)
			assertNull(modRes.shadowedFilePath)
		}

		@Test
		fun `enforces module isolation when dependency is not declared`() {
			val graph = ModuleGraph()
			val modFile = Path.of("libs/network/network.zig").toAbsolutePath().normalize()
			graph.registerModule("network", modFile)

			val fromFile = Path.of("src/main.zig").toAbsolutePath().normalize()
			val callingModule = graph.registerModule(
				name = "caller",
				rootFile = fromFile,
				dependencies = emptyMap(),
			)

			val resolution = graph.resolve(
				currentModule = callingModule,
				currentFile = fromFile,
				importTarget = "network",
			)

			val notFound = assertInstanceOf<ImportResolution.ModuleNotFound>(resolution)
			assertEquals("network", notFound.name)
		}

		@Test
		fun `reports module not found when import target is undeclared module`() {
			val graph = ModuleGraph()
			val fromFile = Path.of("src/main.zig").toAbsolutePath().normalize()
			val module = graph.getOrRegisterModule(fromFile)

			val resolution = graph.resolve(
				currentModule = module,
				currentFile = fromFile,
				importTarget = "unknown_pkg",
			)

			val notFound = assertInstanceOf<ImportResolution.ModuleNotFound>(resolution)
			assertEquals("unknown_pkg", notFound.name)
		}

		@Test
		fun `reports invalid file path when import target has illegal characters`() {
			val graph = ModuleGraph()
			val fromFile = Path.of("src/main.zig").toAbsolutePath().normalize()
			val module = graph.getOrRegisterModule(fromFile)

			val resolution = graph.resolve(
				currentModule = module,
				currentFile = fromFile,
				importTarget = "bad\u0000file.zig",
			)

			val invalid = assertInstanceOf<ImportResolution.InvalidFilePath>(resolution)
			assertEquals("bad\u0000file.zig", invalid.rawTarget)
			assertEquals(FileKind.Zig, invalid.kind)
		}

		@Test
		fun `reports unresolvable context path when importing file has no parent directory`() {
			val graph = ModuleGraph()
			val rootFile = Path.of("/").toAbsolutePath().normalize()

			assumeTrue(rootFile.parent == null)

			val module = graph.getOrRegisterModule(rootFile)

			val resolution = graph.resolve(
				currentModule = module,
				currentFile = rootFile,
				importTarget = "helper.zig",
			)

			val unresolvable = assertInstanceOf<ImportResolution.UnresolvableContextPath>(resolution)
			assertEquals(rootFile, unresolvable.contextFile)
		}
	}
}