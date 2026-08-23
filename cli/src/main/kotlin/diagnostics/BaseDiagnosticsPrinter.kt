package net.landless_city.zigocracy.cli.diagnostics

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.reset
import com.github.ajalt.mordant.terminal.Terminal
import net.landless_city.zigocracy.cli.syntax_highlight.SyntaxHighlightTheme
import net.landless_city.zigocracy.cli.syntax_highlight.TokenStyleApplier
import net.landless_city.zigocracy.zig.cli.EnglishDiagnosticLocalizer
import net.landless_city.zigocracy.zig.lexer.TokenDiagnostic
import net.landless_city.zigocracy.zig.parser.ParserResult
import net.landless_city.zigocracy.zig.shared.DiagnosticCode
import net.landless_city.zigocracy.zig.shared.DiagnosticSeverity
import net.landless_city.zigocracy.zig.syntax.NodeEvent
import net.landless_city.zigocracy.zig.syntax.SyntaxStreamVisitor
import net.landless_city.zigocracy.zig.syntax.TokenEvent
import net.landless_city.zigocracy.zig.text.LineMap
import java.nio.file.Path

internal abstract class BaseDiagnosticsPrinter(
	protected val terminal: Terminal,
	protected val path: Path,
	protected val parserResult: ParserResult,
	protected val theme: SyntaxHighlightTheme
) : SyntaxStreamVisitor {
	private var currentAbsoluteOffset = 0

	final override fun enterNode(index: Int, event: NodeEvent): Boolean = true
	final override fun leaveNode(index: Int, event: NodeEvent) {}

	final override fun visitToken(index: Int, event: TokenEvent, diagnostics: List<TokenDiagnostic>) {
		if (diagnostics.isNotEmpty()) {
			val lineMap = parserResult.source.lineMap
			for (diag in diagnostics) {
				val absoluteErrorOffset = currentAbsoluteOffset + diag.startOffset
				val coordinates = lineMap.getCoordinates(absoluteErrorOffset)

				onDiagnosticFound(diag, coordinates)
				terminal.println()
			}
		}
		currentAbsoluteOffset += event.width
	}

	protected abstract fun onDiagnosticFound(diagnostic: TokenDiagnostic, coordinates: LineMap.Coordinates)

	protected fun buildSeverity(diagnosticCode: DiagnosticCode): String {
		val (style, text) = when (diagnosticCode.severity) {
			DiagnosticSeverity.Error -> (red + bold) to "error"
			DiagnosticSeverity.Warning -> (yellow + bold) to "warning"
		}
		return style(text)
	}

	protected fun buildNote(diagnosticCode: DiagnosticCode): String? {
		val noteContent = EnglishDiagnosticLocalizer.getNote(diagnosticCode) ?: return null
		val notePrefix = (cyan + bold)("note")
		return "$notePrefix: $noteContent"
	}

	protected fun buildSourceLine(lineIndex: Int): String {
		val lineMap = parserResult.source.lineMap
		val lineRange = lineMap.getLineRange(lineIndex)
		var trackerOffset = 0
		val stream = parserResult.stream

		return buildString {
			for (index in stream.events.indices) {
				val event = stream.events[index]
				if (event is TokenEvent) {
					val tokenEnd = trackerOffset + event.width

					if (tokenEnd > lineRange.first && trackerOffset <= lineRange.last) {
						val rawTokenText =
							parserResult.source
								.getTextSlice(trackerOffset, event.width)
								.replace("\n", "")
								.replace("\r", "")

						val tokenDiagnostics = stream.diagnostics[index] ?: emptyList()

						val formattedToken = TokenStyleApplier.applyStyles(rawTokenText, event, tokenDiagnostics, theme)
						append(formattedToken)
					}

					trackerOffset += event.width
					if (trackerOffset > lineRange.last) break
				}
			}
			append(reset(" "))
		}
	}
}