package com.zigocracy.sdk.cli.diagnostics

import com.github.ajalt.mordant.terminal.Terminal
import com.zigocracy.sdk.cli.syntax_highlight.SyntaxHighlightTheme
import com.zigocracy.sdk.engine.vfs.SourceFile
import com.zigocracy.sdk.zig.syntax.SyntaxStream
import java.nio.file.Path

internal interface DiagnosticsFormatter {
	fun report(
		terminal: Terminal,
		path: Path,
		sourceFile: SourceFile,
		stream: SyntaxStream,
		theme: SyntaxHighlightTheme
	)
}