package com.zigocracy.sdk.zig.parser

import com.zigocracy.sdk.zig.lexer.LookaheadTokenReader
import com.zigocracy.sdk.zig.syntax.NodeKind
import com.zigocracy.sdk.zig.syntax.SyntaxStreamBuilder
import com.zigocracy.sdk.zig.text.SourceFile
import com.zigocracy.sdk.zig.text.impl.SourceFileTextStream

object Parser {
	/**
	 * Public entry point that performs top-down syntax analysis and
	 * yields a unified linear syntax event stream with collected diagnostics.
	 */
	fun parseSyntax(source: SourceFile): ParserResult {
		// Inputs
		val textStream = SourceFileTextStream(source)
		val tokenReader = LookaheadTokenReader(textStream)
		// Outputs
		val builder = SyntaxStreamBuilder()

		parseFile(tokenReader, builder)

		return ParserResult(source, builder.build())
	}

	private fun parseFile(
		reader: LookaheadTokenReader,
		builder: SyntaxStreamBuilder
	) {
		val rootMark = builder.recordStart()

		while (true) {
			val (event, diagnostics) = reader.consume() ?: break

			builder.addToken(event.kind, event.width, diagnostics)
		}

		builder.emitNode(rootMark, NodeKind.File)
	}
}