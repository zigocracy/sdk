package com.zigocracy.sdk.cli.syntax_highlight

import com.github.ajalt.mordant.terminal.Terminal
import com.zigocracy.sdk.zig.lexer.TokenDiagnostic
import com.zigocracy.sdk.zig.syntax.NodeEvent
import com.zigocracy.sdk.zig.syntax.SyntaxStreamVisitor
import com.zigocracy.sdk.zig.syntax.TokenEvent
import com.zigocracy.sdk.zig.text.SourceFile

internal class HighlightPrinter(
	private val terminal: Terminal,
	private val sourceFile: SourceFile,
	private val theme: SyntaxHighlightTheme
) : SyntaxStreamVisitor {
	private var currentTextOffset = 0

	override fun enterNode(index: Int, event: NodeEvent) = true

	override fun visitToken(index: Int, event: TokenEvent, diagnostics: List<TokenDiagnostic>) {
		val rawTokenText = sourceFile.getTextSlice(currentTextOffset, event.width)

		val formattedToken = TokenStyleApplier.applyStyles(rawTokenText, event, diagnostics, theme)
		terminal.print(formattedToken)

		currentTextOffset += event.width
	}
}