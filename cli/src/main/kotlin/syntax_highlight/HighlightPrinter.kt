package net.landless_city.zigocracy.cli.syntax_highlight

import com.github.ajalt.mordant.terminal.Terminal
import net.landless_city.zigocracy.zig.lexer.TokenDiagnostic
import net.landless_city.zigocracy.zig.syntax.NodeEvent
import net.landless_city.zigocracy.zig.syntax.SyntaxStreamVisitor
import net.landless_city.zigocracy.zig.syntax.TokenEvent
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

		val formattedToken = TokenStyleApplier.applyStyles(rawTokenText, event, diagnostics, theme)
		terminal.print(formattedToken)

		currentTextOffset += event.width
	}
}