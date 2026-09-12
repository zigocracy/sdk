package com.zigocracy.sdk.cli.diagnostics

import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.Panel
import com.zigocracy.sdk.cli.EnglishDiagnosticLocalizer
import com.zigocracy.sdk.cli.syntax_highlight.SyntaxHighlightTheme
import com.zigocracy.sdk.engine.vfs.LineMap
import com.zigocracy.sdk.engine.vfs.SourceFile
import com.zigocracy.sdk.zig.lexer.TokenDiagnostic
import com.zigocracy.sdk.zig.syntax.SyntaxStream
import com.zigocracy.sdk.zig.syntax.traverseFromRoot
import java.nio.file.Path

internal class RichDiagnosticsFormatter(val contextSize: Int) : DiagnosticsFormatter {
	override fun report(
		terminal: Terminal,
		path: Path,
		sourceFile: SourceFile,
		stream: SyntaxStream,
		theme: SyntaxHighlightTheme
	) {
		val printer = RichStreamPrinter(terminal, path, sourceFile, stream, theme, contextSize)
		stream.traverseFromRoot(printer)
	}
}

internal class RichStreamPrinter(
	terminal: Terminal,
	path: Path,
	sourceFile: SourceFile,
	stream: SyntaxStream,
	theme: SyntaxHighlightTheme,
	val contextSize: Int,
) : BaseDiagnosticsPrinter(terminal, path, sourceFile, stream, theme) {

	override fun onDiagnosticFound(diagnostic: TokenDiagnostic, coordinates: LineMap.Coordinates) {
		val severity = buildSeverity(diagnostic.code)
		val message = EnglishDiagnosticLocalizer.getMessage(diagnostic.code)

		terminal.println("$severity: $message")
		buildNote(diagnostic.code)?.let { note ->
			terminal.println(note)
		}
		printRichContextBox(coordinates.line - 1)
	}

	private fun printRichContextBox(targetLineIndex: Int) {
		val lineMap = sourceFile.lineMap
		val maxLineIndex = lineMap.getLineCount() - 1
		val paddingBefore = (contextSize - 1) / 2
		val paddingAfter = contextSize - 1 - paddingBefore

		val startLineIndex = (targetLineIndex - paddingBefore).coerceAtLeast(0)
		val endLineIndex = (targetLineIndex + paddingAfter).coerceAtMost(maxLineIndex)

		val lineNumberPaddingWidth = (endLineIndex + 1).toString().length

		val innerContent = buildString {
			appendLine()
			for (i in startLineIndex..endLineIndex) {
				val lineNumStr = (i + 1).toString().padStart(lineNumberPaddingWidth)
				val highlightedContent = buildSourceLine(i)

				val formattedGutter = gray("$lineNumStr │ ")
				appendLine(" $formattedGutter$highlightedContent")
			}
		}

		val contextPanel = Panel(
			content = innerContent,

			title = " $path ",
			titleAlign = TextAlign.CENTER,
			borderStyle = gray
		)

		terminal.println(contextPanel)
	}
}