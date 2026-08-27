package com.zigocracy.sdk.cli.syntax_highlight

import com.github.ajalt.mordant.rendering.TextColors.Companion.rgb
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.underline
import com.zigocracy.sdk.zig.shared.DiagnosticCode
import com.zigocracy.sdk.zig.shared.DiagnosticSeverity
import com.zigocracy.sdk.zig.syntax.TokenKind
import com.zigocracy.sdk.zig.syntax.VisualGroup
import com.zigocracy.sdk.zig.syntax.classifyToVisualGroup

internal object LightSyntaxHighlightTheme : SyntaxHighlightTheme {
	override fun getSyntaxStyle(kind: TokenKind): TextStyle = when (kind.classifyToVisualGroup()) {
		VisualGroup.Whitespace, VisualGroup.Newline -> rgb("#A0A0A0")
		VisualGroup.Comment -> rgb("#A0A0A0")
		VisualGroup.DocComment -> rgb("#008000")
		VisualGroup.String -> rgb("#22863A")
		VisualGroup.Number -> rgb("#098658")
		VisualGroup.Identifier -> rgb("#24292E")
		VisualGroup.BuiltinIdentifier -> rgb("#6F42C1")
		VisualGroup.Keyword -> bold + rgb("#D73A49")
		VisualGroup.BadCharacter -> bold + rgb("#CD3131")
		VisualGroup.Operator, VisualGroup.Punctuation -> rgb("#5C6370")
	}

	override fun getDiagnosticUnderline(code: DiagnosticCode): TextStyle {
		val underlineColor = when (code.severity) {
			DiagnosticSeverity.Error -> rgb("#D13438")
			DiagnosticSeverity.Warning -> rgb("#B78103")
		}
		return underline + underlineColor
	}

	override fun getDiagnosticBackgroundHighlight(code: DiagnosticCode): TextStyle {
		return when (code.severity) {
			DiagnosticSeverity.Error -> {
				rgb("#FFF0F0").bg + rgb("#C42B2B") + bold
			}

			DiagnosticSeverity.Warning -> {
				rgb("#FFFFE5").bg + rgb("#8F6B00") + bold
			}
		}
	}
}