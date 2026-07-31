package net.landless_city.zigocracy.zig.parser

import net.landless_city.zigocracy.zig.scanner.ScanResult
import net.landless_city.zigocracy.zig.scanner.impl.*
import net.landless_city.zigocracy.zig.shared.CodeUnits
import net.landless_city.zigocracy.zig.shared.SourceFile
import net.landless_city.zigocracy.zig.syntax.NodeKind
import net.landless_city.zigocracy.zig.syntax.SyntaxStreamBuilder
import net.landless_city.zigocracy.zig.syntax.TokenEvent
import net.landless_city.zigocracy.zig.syntax.TokenKind

object Parser {
	/**
	 * Public entry point that performs top-down syntax analysis and
	 * yields a unified linear syntax event stream with collected diagnostics.
	 */
	fun analyze(source: SourceFile, baseOffset: CodeUnits = 0): ParserResult {
		val cursor = LinearTextCursor(source)
		val builder = SyntaxStreamBuilder()
		val diagnostics = mutableListOf<SyntaxDiagnostic>()

		parseRoot(cursor, builder, diagnostics, baseOffset)

		return ParserResult(source, builder.build(), diagnostics)
	}

	private fun parseRoot(
		cursor: LinearTextCursor,
		builder: SyntaxStreamBuilder,
		diagnostics: MutableList<SyntaxDiagnostic>,
		baseOffset: CodeUnits
	) {
		val rootMark = builder.recordStart()

		while (cursor.peekChar() != null) {
			val (kind, width) = consumeNextTokenAndMapDiagnostics(cursor, diagnostics, baseOffset)
			builder.addToken(kind, width)
		}

		builder.emitNode(rootMark, NodeKind.File)
	}

	private fun consumeNextTokenAndMapDiagnostics(
		cursor: LinearTextCursor,
		diagnostics: MutableList<SyntaxDiagnostic>,
		baseOffset: CodeUnits
	): TokenEvent {
		when (val result = peekNextToken(cursor)) {
			is ScanResult.Matched -> {
				// Direct monotonic map of relative token diagnostics to file coordinates
				if (result.diagnostics.isNotEmpty()) {
					result.diagnostics.mapTo(diagnostics) { diag ->
						SyntaxDiagnostic(
							code = diag.code,
							startPosition = baseOffset + (cursor.textCursor + diag.relativeStart),
							width = diag.width
						)
					}
				}

				val width = result.width
				cursor.advance(width)
				return TokenEvent(result.kind, width)
			}

			is ScanResult.NoMatch -> {
				val width = 1
				cursor.advance(width)
				return TokenEvent(TokenKind.ErrorToken, width)
			}
		}
	}

	// Evaluated in order of grammatical domain priority
	private val scanners = listOf(
		WhitespaceScanner,
		CommentScanner,
		StringScanner,
		MultilineStringScanner,
		CharScanner,
		PunctuationScanner,
		BuiltinIdentiiferScanner,
		IdentifierScanner,
		NumberScanner
	)

	private fun peekNextToken(cursor: LinearTextCursor): ScanResult {
		for (scanner in scanners) {
			val result = scanner.scan(cursor)
			if (result is ScanResult.Matched) {
				return result
			}
		}

		return ScanResult.NoMatch
	}
}