package net.landless_city.zigocracy.zig.parser

import net.landless_city.zigocracy.zig.lexer.LookaheadTokenReader
import net.landless_city.zigocracy.zig.lexer.Tokenizer
import net.landless_city.zigocracy.zig.syntax.NodeKind
import net.landless_city.zigocracy.zig.syntax.SyntaxStreamBuilder
import net.landless_city.zigocracy.zig.syntax.TokenEvent
import net.landless_city.zigocracy.zig.text.CodeUnits
import net.landless_city.zigocracy.zig.text.SourceFile
import net.landless_city.zigocracy.zig.text.TextStream
import net.landless_city.zigocracy.zig.text.impl.SourceFileTextStream

object Parser {
	/**
	 * Public entry point that performs top-down syntax analysis and
	 * yields a unified linear syntax event stream with collected diagnostics.
	 */
	fun analyze(source: SourceFile, baseOffset: CodeUnits = 0): ParserResult {
		// Inputs
		val textStream = SourceFileTextStream(source)
		val tokenReader = LookaheadTokenReader(textStream)
		// Outputs
		val builder = SyntaxStreamBuilder()
		val diagnostics = mutableListOf<SyntaxDiagnostic>()

		parseRoot(textStream, builder, diagnostics, baseOffset)

		return ParserResult(source, builder.build(), diagnostics)
	}

	private fun parseRoot(
		cursor: TextStream,
		builder: SyntaxStreamBuilder,
		diagnostics: MutableList<SyntaxDiagnostic>,
		baseOffset: CodeUnits
	) {
		val rootMark = builder.recordStart()

		while (true) {
			val event = consumeNextTokenAndMapDiagnostics(cursor, diagnostics, baseOffset) ?: break
			builder.addToken(event.kind, event.width)
		}

		builder.emitNode(rootMark, NodeKind.File)
	}

	private fun consumeNextTokenAndMapDiagnostics(
		cursor: TextStream,
		diagnostics: MutableList<SyntaxDiagnostic>,
		baseOffset: CodeUnits
	): TokenEvent? {
		val result = Tokenizer.tokenizeNext(cursor) ?: return null
		val event = result.event
		if (result.diagnostics.isNotEmpty()) {
			// Direct monotonic map of relative token diagnostics to file coordinates
			result.diagnostics.mapTo(diagnostics) { diag ->
				SyntaxDiagnostic(
					code = diag.code,
					startPosition = baseOffset + (cursor.textCursor + diag.startOffset),
					width = diag.width
				)
			}
		}
		cursor.advance(event.width)
		return event
	}
}