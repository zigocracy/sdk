package com.zigocracy.sdk.engine.vfs

import java.io.IOException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Thread-safe Virtual File System managing file snapshots and editor overlays.
 */
class Vfs {
	private val nextFileId = AtomicInteger(1)
	private val pathToId = ConcurrentHashMap<Path, FileId>()
	private val idToPath = ConcurrentHashMap<FileId, Path>()

	private val activeSnapshots = ConcurrentHashMap<FileId, SourceFile>()
	private val revisions = ConcurrentHashMap<FileId, AtomicInteger>()
	private val overlayFiles = ConcurrentHashMap.newKeySet<FileId>()

	/**
	 * Canonicalizes a path using lexical normalization.
	 *
	 * Time Complexity: Θ(N), where N is the number of path segments.
	 * Memory Complexity: Θ(N)
	 */
	fun canonicalize(path: Path): Path = path.toAbsolutePath().normalize()

	/**
	 * Returns or registers a unique [FileId] for the given path.
	 *
	 * Time Complexity: Θ(1) amortized.
	 * Memory Complexity: Θ(1) amortized.
	 */
	fun getOrRegisterId(path: Path): FileId {
		val canonical = canonicalize(path)
		return pathToId.computeIfAbsent(canonical) {
			val id = FileId(nextFileId.getAndIncrement())
			idToPath[id] = canonical
			id
		}
	}

	fun getPath(id: FileId): Path? = idToPath[id]

	/**
	 * Sets or updates an in-memory overlay, advancing the file's revision.
	 *
	 * Time Complexity: Θ(N), where N is the text length.
	 * Memory Complexity: Θ(N)
	 */
	fun setOverlay(path: Path, text: String, originalPath: String = path.toString()): SourceFile {
		val canonical = canonicalize(path)
		val fileId = getOrRegisterId(canonical)
		val revSeq = revisions.computeIfAbsent(fileId) { AtomicInteger(0) }
		val newRev = revSeq.incrementAndGet()

		val snapshot = SourceFile(
			id = fileId,
			path = canonical,
			text = text,
			revision = newRev,
			originalPath = originalPath,
		)
		overlayFiles.add(fileId)
		activeSnapshots[fileId] = snapshot
		return snapshot
	}

	/**
	 * Removes an in-memory overlay, reverting to disk on next access.
	 *
	 * Time Complexity: Θ(1)
	 * Memory Complexity: Θ(1)
	 */
	fun removeOverlay(path: Path) {
		val canonical = canonicalize(path)
		val fileId = pathToId[canonical] ?: return
		overlayFiles.remove(fileId)
		activeSnapshots.remove(fileId)
	}

	fun hasOverlay(path: Path): Boolean {
		val canonical = canonicalize(path)
		val fileId = pathToId[canonical] ?: return false
		return overlayFiles.contains(fileId)
	}

	/**
	 * Retrieves an immutable snapshot of a file.
	 *
	 * In-memory overlays take precedence; reads from disk on cache miss.
	 *
	 * Time Complexity: Θ(1) hit, Θ(N) miss where N is file size on disk.
	 * Memory Complexity: Θ(1) hit, Θ(N) miss.
	 */
	fun acquire(path: Path, originalPath: String = path.toString()): VfsResult {
		val canonical = canonicalize(path)
		val fileId = getOrRegisterId(canonical)

		val existing = activeSnapshots[fileId]
		if (existing != null) {
			return VfsResult.Success(existing)
		}

		return try {
			if (!Files.isRegularFile(canonical)) {
				return VfsResult.NotFound(canonical)
			}
			val text = Files.readString(canonical)
			val revSeq = revisions.computeIfAbsent(fileId) { AtomicInteger(0) }
			val snapshot = SourceFile(
				id = fileId,
				path = canonical,
				text = text,
				revision = revSeq.get(),
				originalPath = originalPath,
			)
			activeSnapshots[fileId] = snapshot
			VfsResult.Success(snapshot)
		} catch (e: NoSuchFileException) {
			VfsResult.NotFound(canonical)
		} catch (e: IOException) {
			VfsResult.ReadError(canonical, e)
		}
	}

	fun getActiveSnapshot(id: FileId): SourceFile? = activeSnapshots[id]

	fun getActiveSnapshot(path: Path): SourceFile? {
		val canonical = canonicalize(path)
		val id = pathToId[canonical] ?: return null
		return activeSnapshots[id]
	}
}