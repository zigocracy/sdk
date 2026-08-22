package net.landless_city.zigocracy.cli.highlight_syntax

import com.github.ajalt.mordant.rendering.TextColors.Companion.rgb
import com.github.ajalt.mordant.rendering.TextColors.black
import com.github.ajalt.mordant.rendering.TextColors.white
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.underline
import net.landless_city.zigocracy.zig.shared.DiagnosticCode
import net.landless_city.zigocracy.zig.shared.DiagnosticSeverity
import net.landless_city.zigocracy.zig.syntax.TokenKind
import net.landless_city.zigocracy.zig.syntax.VisualGroup
import net.landless_city.zigocracy.zig.syntax.classifyToVisualGroup

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
		VisualGroup.BadCharacter -> bold + rgb("#F44747")
		VisualGroup.Operator, VisualGroup.Punctuation -> rgb("#DCDCAA")
	}

	override fun getDiagnosticUnderline(code: DiagnosticCode): TextStyle {
		val underlineColor = when (code.severity) {
			DiagnosticSeverity.Error -> rgb("#F44747")
			DiagnosticSeverity.Warning -> rgb("#CCA700")
		}
		return underline + underlineColor
	}

	override fun getDiagnosticBackgroundHighlight(code: DiagnosticCode): TextStyle {
		return when (code.severity) {
			DiagnosticSeverity.Error -> rgb("#A61C1C").bg + white + bold
			DiagnosticSeverity.Warning -> rgb("#D7A100").bg + black + bold
		}
	}
}
