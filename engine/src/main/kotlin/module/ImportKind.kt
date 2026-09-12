package com.zigocracy.sdk.engine.module

import java.nio.file.InvalidPathException
import java.nio.file.Path

/**
 * Classification of an `@import(...)` statement target in Zig.
 */
sealed interface ImportKind {
	/**
	 * File import relative to the current file's directory.
	 */
	@JvmRecord
	data class File(val path: Path, val kind: FileKind) : ImportKind

	/**
	 * Module import resolved via the module dependency graph.
	 */
	@JvmRecord
	data class Module(val name: String) : ImportKind

	/**
	 * File import target with valid extension but invalid filesystem path characters.
	 */
	@JvmRecord
	data class InvalidFilePath(val rawTarget: String, val kind: FileKind, val reason: String) : ImportKind

	companion object {
		/**
		 * Classifies an import target:
		 * - Ends with `.zig` -> Zig source file.
		 * - Ends with `.zon` -> ZON data file.
		 * - Otherwise -> named module.
		 *
		 * Matching is case-sensitive.
		 *
		 * Time Complexity: Θ(L), where L is the length of the target string.
		 * Memory Complexity: Θ(L)
		 */
		fun classify(target: String): ImportKind {
			val kind = when {
				target.endsWith(".zig", ignoreCase = false) -> FileKind.Zig
				target.endsWith(".zon", ignoreCase = false) -> FileKind.Zon
				else -> null
			}

			if (kind == null) {
				return Module(target)
			}

			return try {
				File(Path.of(target), kind)
			} catch (e: InvalidPathException) {
				InvalidFilePath(
					rawTarget = target,
					kind = kind,
					reason = e.reason ?: "Invalid filesystem path syntax"
				)
			}
		}
	}
}