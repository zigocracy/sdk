package com.zigocracy.sdk.engine

import com.zigocracy.sdk.engine.vfs.ContentHash
import com.zigocracy.sdk.engine.vfs.SourceFile
import com.zigocracy.sdk.zig.parser.Parser
import com.zigocracy.sdk.zig.syntax.SyntaxStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Caches parsed syntax streams by document content hash to avoid redundant parsing.
 */
class ParseCache {
	private val entries = ConcurrentHashMap<ContentHash, SyntaxStream>()

	/**
	 * Returns the parsed [SyntaxStream] for the file, parsing on demand if not cached.
	 *
	 * Time Complexity: Θ(1) cache hit, Θ(N) parse on miss.
	 * Memory Complexity: Θ(1) hit, Θ(N) miss.
	 */
	fun getOrParse(source: SourceFile): SyntaxStream {
		return entries.computeIfAbsent(source.contentHash) {
			Parser.parseSyntax(source.text)
		}
	}

	fun invalidate(contentHash: ContentHash) {
		entries.remove(contentHash)
	}

	fun clear() {
		entries.clear()
	}
}