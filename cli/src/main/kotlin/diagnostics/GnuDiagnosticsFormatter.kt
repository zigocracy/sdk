package com.zigocracy.sdk.cli.diagnostics

import com.github.ajalt.mordant.terminal.Terminal
import com.zigocracy.sdk.cli.EnglishDiagnosticLocalizer
import com.zigocracy.sdk.cli.syntax_highlight.SyntaxHighlightTheme
import com.zigocracy.sdk.zig.lexer.TokenDiagnostic
import com.zigocracy.sdk.zig.parser.ParserResult
import com.zigocracy.sdk.zig.syntax.traverseFromRoot
import com.zigocracy.sdk.zig.text.LineMap
import java.nio.file.Path

internal object GnuDiagnosticsFormatter : DiagnosticsFormatter {

	override fun report(
		terminal: Terminal,
		path: Path,
		parserResult: ParserResult,
		theme: SyntaxHighlightTheme
	) {
		val printer = GnuStreamPrinter(terminal, path, parserResult, theme)
		parserResult.stream.traverseFromRoot(printer)
	}
}

internal class GnuStreamPrinter(
	terminal: Terminal,
	path: Path,
	parserResult: ParserResult,
	theme: SyntaxHighlightTheme
) : BaseDiagnosticsPrinter(terminal, path, parserResult, theme) {

	override fun onDiagnosticFound(diagnostic: TokenDiagnostic, coordinates: LineMap.Coordinates) {
		val severity = buildSeverity(diagnostic.code)
		val message = EnglishDiagnosticLocalizer.getMessage(diagnostic.code)

		terminal.println("$path:${coordinates.line}:${coordinates.column}: $severity: $message")
		terminal.println(buildSourceLine(lineIndex = coordinates.line - 1))
		buildNote(diagnostic.code)?.let { note ->
			terminal.println(note)
		}
	}
}