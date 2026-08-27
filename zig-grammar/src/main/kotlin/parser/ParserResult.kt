package com.zigocracy.sdk.zig.parser

import com.zigocracy.sdk.zig.syntax.SyntaxStream
import com.zigocracy.sdk.zig.text.SourceFile

/**
 * Holds the results of a [Parser] run.
 */
@JvmRecord
data class ParserResult(
	val source: SourceFile,
	val stream: SyntaxStream,
)