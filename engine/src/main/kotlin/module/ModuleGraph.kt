package com.zigocracy.sdk.engine.module

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Dependency graph of Zig modules.
 */
class ModuleGraph {
	private val nextModuleId = AtomicInteger(1)
	private val modules = ConcurrentHashMap<ModuleId, Module>()
	private val moduleByName = ConcurrentHashMap<String, ModuleId>()
	private val rootToModule = ConcurrentHashMap<Path, ModuleId>()

	/**
	 * Registers a module with its root file and dependencies.
	 *
	 * Explicit [dependencies] override [implicitDependencies] of the same name.
	 *
	 * Time Complexity: Θ(D), where D is the number of dependencies.
	 * Memory Complexity: Θ(D)
	 */
	fun registerModule(
		name: String,
		rootFile: Path,
		dependencies: Map<String, ModuleId> = emptyMap(),
		implicitDependencies: Map<String, ModuleId> = emptyMap(),
		rootDirectory: Path? = null,
	): Module {
		val canonicalRoot = rootFile.toAbsolutePath().normalize()
		val resolvedRootDir = rootDirectory?.toAbsolutePath()?.normalize()
			?: canonicalRoot.parent
			?: canonicalRoot

		val combinedDependencies = buildMap(implicitDependencies.size + dependencies.size) {
			putAll(implicitDependencies)
			putAll(dependencies)
		}

		val id = ModuleId(nextModuleId.getAndIncrement())
		val module = Module(
			id = id,
			name = name,
			rootFile = canonicalRoot,
			rootDirectory = resolvedRootDir,
			dependencies = combinedDependencies,
		)
		modules[id] = module
		moduleByName[name] = id
		rootToModule[canonicalRoot] = id
		return module
	}

	/**
	 * Returns an existing module registered for [rootFile], or registers a new module
	 * with [name] (defaults to "main") and the file's parent directory as its root.
	 */
	fun getOrRegisterModule(rootFile: Path, name: String = "main"): Module {
		val canonical = rootFile.toAbsolutePath().normalize()
		return findModuleByRootFile(canonical) ?: registerModule(
			name = name,
			rootFile = canonical,
			rootDirectory = canonical.parent ?: canonical,
		)
	}

	fun getModule(id: ModuleId): Module? = modules[id]

	fun getAllModules(): Collection<Module> = modules.values

	fun findModuleByName(name: String): Module? {
		val id = moduleByName[name] ?: return null
		return modules[id]
	}

	fun findModuleByRootFile(path: Path): Module? {
		val canonical = path.toAbsolutePath().normalize()
		val id = rootToModule[canonical] ?: return null
		return modules[id]
	}

	/**
	 * Resolves an `@import(...)` [importTarget] string originating from [currentFile]
	 * within [currentModule].
	 *
	 * Module dependencies take precedence over local files.
	 *
	 * Time Complexity: Θ(L), where L is the import path length.
	 * Memory Complexity: Θ(L)
	 */
	fun resolve(
		currentModule: Module,
		currentFile: Path,
		importTarget: String,
	): ImportResolution {
		val targetModuleId = currentModule.dependencies[importTarget]

		if (targetModuleId != null) {
			val targetModule = modules[targetModuleId]
			if (targetModule != null) {
				val shadowed = findCoexistingLocalFile(currentFile, importTarget)
				return ImportResolution.ModuleTarget(targetModule, shadowedFilePath = shadowed)
			}
		}

		val importKind = ImportKind.classify(importTarget)
		return when (importKind) {
			is ImportKind.InvalidFilePath -> {
				ImportResolution.InvalidFilePath(
					rawTarget = importKind.rawTarget,
					kind = importKind.kind,
					reason = importKind.reason
				)
			}

			is ImportKind.File -> {
				val parentDir = currentFile.toAbsolutePath().normalize().parent
					?: return ImportResolution.UnresolvableContextPath(
						contextFile = currentFile,
						target = importKind.path,
						kind = importKind.kind,
						reason = "Importing file has no parent directory"
					)

				val resolvedPath = parentDir.resolve(importKind.path).normalize()
				if (!resolvedPath.startsWith(currentModule.rootDirectory)) {
					return ImportResolution.FileOutsideModule(
						targetPath = resolvedPath,
						moduleRootDirectory = currentModule.rootDirectory,
						kind = importKind.kind,
					)
				}

				ImportResolution.File(resolvedPath, kind = importKind.kind)
			}

			is ImportKind.Module -> {
				ImportResolution.ModuleNotFound(importKind.name)
			}
		}
	}

	private fun findCoexistingLocalFile(importingFile: Path, target: String): Path? {
		return try {
			val parent = importingFile.toAbsolutePath().normalize().parent ?: return null
			val candidate = parent.resolve(Path.of(target)).normalize()
			if (Files.isRegularFile(candidate)) candidate else null
		} catch (e: Exception) {
			null
		}
	}
}