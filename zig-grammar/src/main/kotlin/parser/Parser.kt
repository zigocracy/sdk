package com.zigocracy.sdk.zig.parser

import com.zigocracy.sdk.zig.lexer.LookaheadTokenReader
import com.zigocracy.sdk.zig.syntax.NodeKind
import com.zigocracy.sdk.zig.syntax.SyntaxStream
import com.zigocracy.sdk.zig.syntax.SyntaxStreamBuilder
import com.zigocracy.sdk.zig.text.TextStream
import com.zigocracy.sdk.zig.text.impl.StringTextStream

object Parser {
	/**
	 * Public entry point that performs top-down syntax analysis on a [TextStream] and
	 * yields a unified linear syntax event stream with collected diagnostics.
	 */
	fun parseSyntax(stream: TextStream): SyntaxStream {
		val tokenReader = LookaheadTokenReader(stream)
		val builder = SyntaxStreamBuilder()

		parseFile(tokenReader, builder)

		return builder.build()
	}

	/**
	 * Convenience entry point that performs top-down syntax analysis directly on a raw [String].
	 */
	fun parseSyntax(text: String): SyntaxStream = parseSyntax(StringTextStream(text))

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