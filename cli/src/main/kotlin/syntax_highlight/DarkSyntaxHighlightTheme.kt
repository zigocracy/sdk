package com.zigocracy.sdk.cli.syntax_highlight

import com.github.ajalt.mordant.rendering.TextColors.Companion.rgb
import com.github.ajalt.mordant.rendering.TextColors.white
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.underline
import com.zigocracy.sdk.zig.shared.DiagnosticCode
import com.zigocracy.sdk.zig.shared.DiagnosticSeverity
import com.zigocracy.sdk.zig.syntax.TokenKind
import com.zigocracy.sdk.zig.syntax.VisualGroup
import com.zigocracy.sdk.zig.syntax.classifyToVisualGroup

internal object DarkSyntaxHighlightTheme : SyntaxHighlightTheme {
	override fun getSyntaxStyle(kind: TokenKind): TextStyle = when (kind.classifyToVisualGroup()) {
		VisualGroup.Whitespace, VisualGroup.Newline -> rgb("#505050")
		VisualGroup.Comment -> rgb("#768A6B")
		VisualGroup.DocComment -> rgb("#6A9955")
		VisualGroup.String -> rgb("#CE9178")
		VisualGroup.Number -> rgb("#B5CEA8")
		VisualGroup.Identifier -> rgb("#D4D4D4")
		VisualGroup.BuiltinIdentifier -> rgb("#DCDCAA")
		VisualGroup.Keyword -> bold + rgb("#569CD6")
		VisualGroup.BadCharacter -> bold + rgb("#D16969")
		VisualGroup.Operator, VisualGroup.Punctuation -> rgb("#DCDCAA")
	}

	override fun getDiagnosticUnderline(code: DiagnosticCode): TextStyle {
		val underlineColor = when (code.severity) {
			DiagnosticSeverity.Error -> rgb("#E35B5B")
			DiagnosticSeverity.Warning -> rgb("#D1B24F")
		}
		return underline + underlineColor
	}

	override fun getDiagnosticBackgroundHighlight(code: DiagnosticCode): TextStyle {
		return when (code.severity) {
			DiagnosticSeverity.Error -> rgb("#541B1B").bg + white + bold
			DiagnosticSeverity.Warning -> rgb("#4D3D18").bg + white + bold
		}
	}
}