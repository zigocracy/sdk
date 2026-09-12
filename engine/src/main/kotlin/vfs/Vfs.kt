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

	/**
	 * Invalidates the cached disk snapshot for [path], forcing the next [acquire]
	 * to reload from disk.
	 *
	 * If an in-memory editor overlay is active, this method is a no-op and returns `false`,
	 * because editor overlays represent authoritative in-memory state over disk.
	 *
	 * @return `true` if a disk snapshot was invalidated; `false` otherwise.
	 *
	 * Time Complexity: Θ(1)
	 * Memory Complexity: Θ(1)
	 */
	fun invalidate(path: Path): Boolean {
		val canonical = canonicalize(path)
		val fileId = pathToId[canonical] ?: return false
		if (overlayFiles.contains(fileId)) {
			return false
		}
		val removed = activeSnapshots.remove(fileId) != null
		if (removed) {
			revisions[fileId]?.incrementAndGet()
		}
		return removed
	}

	/**
	 * Proactively refreshes [path] from disk.
	 *
	 * If an in-memory editor overlay is active, the overlay snapshot is returned directly.
	 * Otherwise, any existing cached snapshot is evicted and the file is immediately re-read
	 * from disk, advancing its revision if previously cached.
	 *
	 * Time Complexity: Θ(1) hit for overlay, Θ(N) where N is file size on disk.
	 * Memory Complexity: Θ(1) hit for overlay, Θ(N) for disk read.
	 */
	fun refresh(path: Path, originalPath: String = path.toString()): VfsResult {
		val canonical = canonicalize(path)
		val fileId = getOrRegisterId(canonical)
		if (!overlayFiles.contains(fileId)) {
			val hadSnapshot = activeSnapshots.remove(fileId) != null
			if (hadSnapshot) {
				revisions[fileId]?.incrementAndGet()
			}
		}
		return acquire(canonical, originalPath)
	}

	/**
	 * Deletes [path] from the virtual file system, clearing active overlays and cached snapshots.
	 *
	 * @return `true` if an active overlay or snapshot was removed; `false` otherwise.
	 *
	 * Time Complexity: Θ(1)
	 * Memory Complexity: Θ(1)
	 */
	fun delete(path: Path): Boolean {
		val canonical = canonicalize(path)
		val fileId = pathToId[canonical] ?: return false
		val hadOverlay = overlayFiles.remove(fileId)
		val hadSnapshot = activeSnapshots.remove(fileId) != null
		if (hadOverlay || hadSnapshot) {
			revisions[fileId]?.incrementAndGet()
		}
		return hadOverlay || hadSnapshot
	}

	fun getActiveSnapshot(id: FileId): SourceFile? = activeSnapshots[id]

	fun getActiveSnapshot(path: Path): SourceFile? {
		val canonical = canonicalize(path)
		val id = pathToId[canonical] ?: return null
		return activeSnapshots[id]
	}
}
