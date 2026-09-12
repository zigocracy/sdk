package com.zigocracy.sdk.engine.module

import java.nio.file.Path

/**
 * Represents a Zig module with a designated root source file and declared dependencies.
 */
@JvmRecord
data class Module(
	val id: ModuleId,
	val name: String,
	val rootFile: Path,
	val rootDirectory: Path = rootFile.parent ?: rootFile,
	val dependencies: Map<String, ModuleId> = emptyMap(),
)