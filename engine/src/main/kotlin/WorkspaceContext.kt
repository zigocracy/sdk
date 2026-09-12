package com.zigocracy.sdk.engine

import com.zigocracy.sdk.engine.module.ModuleGraph
import com.zigocracy.sdk.engine.vfs.SourceFile
import com.zigocracy.sdk.engine.vfs.Vfs
import com.zigocracy.sdk.engine.vfs.VfsResult
import java.nio.file.Path

/**
 * Context of a Zig workspace, coordinating file snapshots, module dependencies,
 * and cached syntax trees.
 */
class WorkspaceContext internal constructor(
	val rootPath: Path?,
	val vfs: Vfs,
	val moduleGraph: ModuleGraph,
	val parseCache: ParseCache,
) {
	/**
	 * Parses a source file snapshot, reusing cached syntax if the content hash matches.
	 */
	fun parse(source: SourceFile): ParsedFile {
		val stream = parseCache.getOrParse(source)
		return ParsedFile(source, stream)
	}

	/**
	 * Acquires a file from the workspace VFS and parses it.
	 */
	fun parse(path: Path): ParsedFile? {
		val result = vfs.acquire(path)
		return (result as? VfsResult.Success)?.let { parse(it.file) }
	}

	companion object {
		/**
		 * Creates an isolated workspace context for the specified root path.
		 */
		fun create(rootPath: Path? = null): WorkspaceContext = WorkspaceContext(
			rootPath = rootPath?.toAbsolutePath()?.normalize(),
			vfs = Vfs(),
			moduleGraph = ModuleGraph(),
			parseCache = ParseCache(),
		)
	}
}