package com.zigocracy.sdk.cli.syntax_highlight

import com.github.ajalt.mordant.rendering.TextStyle
import com.zigocracy.sdk.zig.shared.DiagnosticCode
import com.zigocracy.sdk.zig.syntax.TokenKind

internal interface SyntaxHighlightTheme {
	fun getSyntaxStyle(kind: TokenKind): TextStyle
	fun getDiagnosticUnderline(code: DiagnosticCode): TextStyle
	fun getDiagnosticBackgroundHighlight(code: DiagnosticCode): TextStyle
}