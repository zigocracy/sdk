package net.landless_city.zigocracy.cli.diagnostics

import com.github.ajalt.mordant.terminal.Terminal
import net.landless_city.zigocracy.cli.syntax_highlight.SyntaxHighlightTheme
import net.landless_city.zigocracy.zig.cli.EnglishDiagnosticLocalizer
import net.landless_city.zigocracy.zig.lexer.TokenDiagnostic
import net.landless_city.zigocracy.zig.parser.ParserResult
import net.landless_city.zigocracy.zig.syntax.traverseFromRoot
import net.landless_city.zigocracy.zig.text.LineMap
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