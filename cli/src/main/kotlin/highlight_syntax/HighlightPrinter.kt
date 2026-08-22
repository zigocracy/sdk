package net.landless_city.zigocracy.cli.highlight_syntax

import com.github.ajalt.mordant.terminal.Terminal
import net.landless_city.zigocracy.zig.lexer.TokenDiagnostic
import net.landless_city.zigocracy.zig.syntax.*
import net.landless_city.zigocracy.zig.text.SourceFile

internal class HighlightPrinter(
	private val terminal: Terminal,
	private val sourceFile: SourceFile,
	private val theme: SyntaxHighlightTheme
) : SyntaxStreamVisitor {
	private var currentTextOffset = 0

	override fun enterNode(index: Int, event: NodeEvent) = true

	override fun visitToken(index: Int, event: TokenEvent, diagnostics: List<TokenDiagnostic>) {
		val rawTokenText = sourceFile.getTextSlice(currentTextOffset, event.width)
		val baseStyle = theme.getSyntaxStyle(event.kind)

		if (diagnostics.isEmpty()) {
			terminal.print(baseStyle(rawTokenText))
		} else {
			val primaryDiagnostic = diagnostics.minByOrNull { it.code.severity } ?: diagnostics.first()
			val errorTokenStyle = baseStyle + theme.getDiagnosticUnderline(primaryDiagnostic.code)

			// Get diagnostics that point to more precise parts of token
			val visualDiagnostics = diagnostics.sortedBy { it.startOffset }

			var cursor = 0
			for (diag in visualDiagnostics) {
				if (diag.startOffset > cursor) {
					val leftPart = rawTokenText.substring(cursor, diag.startOffset)
					terminal.print(errorTokenStyle(leftPart))
					cursor = diag.startOffset
				}

				val errorPartStyle = theme.getDiagnosticUnderline(primaryDiagnostic.code) +
					theme.getDiagnosticBackgroundHighlight(diag.code)

				if (diag.width == 0) {
					val targetPos = diag.startOffset.coerceAtLeast(cursor)
					if (targetPos < rawTokenText.length) {
						val errorPart = rawTokenText.substring(targetPos, targetPos + 1)
						terminal.print(errorPartStyle(errorPart))
						cursor = targetPos + 1
					} else {
						terminal.print(errorPartStyle(" "))
						cursor = rawTokenText.length
					}
				} else {
					val end = diag.endOffset.coerceIn(cursor, rawTokenText.length)
					if (end > cursor) {
						val errorPart = rawTokenText.substring(cursor, end)
						terminal.print(errorPartStyle(errorPart))
						cursor = end
					}
				}
			}

			if (cursor < rawTokenText.length) {
				val rightPart = rawTokenText.substring(cursor)
				terminal.print(errorTokenStyle(rightPart))
			}
		}

		currentTextOffset += event.width
	}
}