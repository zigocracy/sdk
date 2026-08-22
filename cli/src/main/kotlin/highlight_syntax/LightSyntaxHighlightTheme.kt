package net.landless_city.zigocracy.cli.highlight_syntax

import com.github.ajalt.mordant.rendering.TextColors.Companion.rgb
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.underline
import net.landless_city.zigocracy.zig.shared.DiagnosticCode
import net.landless_city.zigocracy.zig.shared.DiagnosticSeverity
import net.landless_city.zigocracy.zig.syntax.TokenKind
import net.landless_city.zigocracy.zig.syntax.VisualGroup
import net.landless_city.zigocracy.zig.syntax.classifyToVisualGroup

internal object LightSyntaxHighlightTheme : SyntaxHighlightTheme {
	override fun getSyntaxStyle(kind: TokenKind): TextStyle = when (kind.classifyToVisualGroup()) {
		VisualGroup.Whitespace, VisualGroup.Newline -> rgb("#A0A0A0")
		VisualGroup.Comment -> rgb("#A0A0A0")
		VisualGroup.DocComment -> rgb("#008000")
		VisualGroup.String -> rgb("#A31515")
		VisualGroup.Number -> rgb("#098658")
		VisualGroup.Identifier -> rgb("#000000")
		VisualGroup.BuiltinIdentifier -> rgb("#795E26")
		VisualGroup.Keyword -> bold + rgb("#0000FF")
		VisualGroup.BadCharacter -> bold + rgb("#CD3131")
		VisualGroup.Operator, VisualGroup.Punctuation -> rgb("#AF00DB")
	}

	override fun getDiagnosticUnderline(code: DiagnosticCode): TextStyle {
		val underlineColor = when (code.severity) {
			DiagnosticSeverity.Error -> rgb("#E51400")
			DiagnosticSeverity.Warning -> rgb("#E5B500")
		}
		return underline + underlineColor
	}

	override fun getDiagnosticBackgroundHighlight(code: DiagnosticCode): TextStyle {
		return when (code.severity) {
			DiagnosticSeverity.Error -> {
				rgb("#FFD2D2").bg + rgb("#B90000") + bold
			}

			DiagnosticSeverity.Warning -> {
				rgb("#FFF2CC").bg + rgb("#7A5A00") + bold
			}
		}
	}
}