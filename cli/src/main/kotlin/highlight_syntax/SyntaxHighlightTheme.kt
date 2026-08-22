package net.landless_city.zigocracy.cli.highlight_syntax

import com.github.ajalt.mordant.rendering.TextStyle
import net.landless_city.zigocracy.zig.shared.DiagnosticCode
import net.landless_city.zigocracy.zig.syntax.TokenKind

internal interface SyntaxHighlightTheme {
	fun getSyntaxStyle(kind: TokenKind): TextStyle
	fun getDiagnosticUnderline(code: DiagnosticCode): TextStyle
	fun getDiagnosticBackgroundHighlight(code: DiagnosticCode): TextStyle
}