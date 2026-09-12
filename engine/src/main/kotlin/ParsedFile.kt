package com.zigocracy.sdk.engine

import com.zigocracy.sdk.engine.vfs.SourceFile
import com.zigocracy.sdk.zig.syntax.SyntaxStream

/**
 * Immutable pairing of a source document snapshot and its parsed syntax event stream.
 */
@JvmRecord
data class ParsedFile(
	val source: SourceFile,
	val stream: SyntaxStream,
) {
	val text: String
		get() = source.text
}