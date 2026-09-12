package com.zigocracy.sdk.cli.diagnostics

import com.github.ajalt.mordant.terminal.Terminal
import com.zigocracy.sdk.cli.EnglishDiagnosticLocalizer
import com.zigocracy.sdk.cli.syntax_highlight.SyntaxHighlightTheme
import com.zigocracy.sdk.engine.vfs.LineMap
import com.zigocracy.sdk.engine.vfs.SourceFile
import com.zigocracy.sdk.zig.lexer.TokenDiagnostic
import com.zigocracy.sdk.zig.syntax.SyntaxStream
import com.zigocracy.sdk.zig.syntax.traverseFromRoot
import java.nio.file.Path

internal object GnuDiagnosticsFormatter : DiagnosticsFormatter {

	override fun report(
		terminal: Terminal,
		path: Path,
		sourceFile: SourceFile,
		stream: SyntaxStream,
		theme: SyntaxHighlightTheme
	) {
		val printer = GnuStreamPrinter(terminal, path, sourceFile, stream, theme)
		stream.traverseFromRoot(printer)
	}
}

internal class GnuStreamPrinter(
	terminal: Terminal,
	path: Path,
	sourceFile: SourceFile,
	stream: SyntaxStream,
	theme: SyntaxHighlightTheme
) : BaseDiagnosticsPrinter(terminal, path, sourceFile, stream, theme) {

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