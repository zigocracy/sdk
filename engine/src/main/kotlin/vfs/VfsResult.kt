package com.zigocracy.sdk.engine.vfs

import java.nio.file.Path

sealed interface VfsResult {
	@JvmRecord
	data class Success(val file: SourceFile) : VfsResult

	@JvmRecord
	data class NotFound(val path: Path) : VfsResult

	@JvmRecord
	data class ReadError(val path: Path, val cause: Throwable) : VfsResult
}