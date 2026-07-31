package net.landless_city.zigocracy.zig.parser

import net.landless_city.zigocracy.zig.syntax.SyntaxStream
import net.landless_city.zigocracy.zig.text.SourceFile

/**
 * Holds the results of a [Parser] run.
 */
@JvmRecord
data class ParserResult(
	val source: SourceFile,
	val stream: SyntaxStream,
)