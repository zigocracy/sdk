package net.landless_city.zigocracy.cli.diagnostics

import com.github.ajalt.mordant.terminal.Terminal
import net.landless_city.zigocracy.cli.syntax_highlight.SyntaxHighlightTheme
import net.landless_city.zigocracy.zig.parser.ParserResult
import java.nio.file.Path

internal interface DiagnosticsFormatter {
	fun report(
		terminal: Terminal,
		path: Path,
		parserResult: ParserResult,
		theme: SyntaxHighlightTheme
	)
}