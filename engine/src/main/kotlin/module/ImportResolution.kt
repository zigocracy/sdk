package com.zigocracy.sdk.engine.module

import java.nio.file.Path

/**
 * Result of resolving an `@import(...)` statement in a [ModuleGraph].
 */
sealed interface ImportResolution {
	/**
	 * Successfully resolved file path import.
	 */
	@JvmRecord
	data class File(val targetPath: Path, val kind: FileKind) : ImportResolution

	/**
	 * Successfully resolved module import from the dependency graph.
	 *
	 * [shadowedFilePath] is non-null when a local file exists with the same name.
	 */
	@JvmRecord
	data class ModuleTarget(
		val module: Module,
		val shadowedFilePath: Path? = null,
	) : ImportResolution

	/**
	 * Module was not found in declared dependencies or the graph.
	 */
	@JvmRecord
	data class ModuleNotFound(val name: String) : ImportResolution

	/**
	 * File import target resolves to a path outside the importing module's root directory.
	 */
	@JvmRecord
	data class FileOutsideModule(
		val targetPath: Path,
		val moduleRootDirectory: Path,
		val kind: FileKind,
	) : ImportResolution

	/**
	 * Import target contained invalid filesystem path characters.
	 */
	@JvmRecord
	data class InvalidFilePath(val rawTarget: String, val kind: FileKind, val reason: String) : ImportResolution

	/**
	 * Path is valid, but cannot be resolved from the importing file's location.
	 *
	 * For example, the importing file has no parent directory.
	 */
	@JvmRecord
	data class UnresolvableContextPath(
		val contextFile: Path,
		val target: Path,
		val kind: FileKind,
		val reason: String,
	) : ImportResolution
}