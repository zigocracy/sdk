package com.zigocracy.sdk.lsp.analysis

import com.zigocracy.sdk.zig.shared.DiagnosticPresentation
import com.zigocracy.sdk.zig.shared.EnglishLspDiagnosticLocalizer
import com.zigocracy.sdk.zig.syntax.TokenEvent
import org.eclipse.lsp4j.*
import com.zigocracy.sdk.zig.shared.DiagnosticSeverity as ZigSeverity
import org.eclipse.lsp4j.DiagnosticSeverity as LspSeverity

internal class LspDiagnosticCollector(
	private val snapshot: DocumentSnapshot,
	private val supportsRelatedInformation: Boolean
) {
	fun collectAndEncode(uri: String): List<Diagnostic> {
		val lspDiagnostics = mutableListOf<Diagnostic>()
		val stream = snapshot.stream
		val lineMap = snapshot.source.lineMap
		var currentAbsoluteOffset = 0

		for (index in stream.events.indices) {
			val event = stream.events[index]
			if (event is TokenEvent) {
				stream.diagnostics[index]?.forEach { diag ->
					val absOffset = currentAbsoluteOffset + diag.startOffset
					val coords = lineMap.getCoordinates(absOffset)

					val startPos = Position(coords.line - 1, coords.column - 1)
					val endPos = Position(coords.line - 1, coords.column - 1 + diag.width)
					val tokenRange = Range(startPos, endPos)

					val lspSeverity = mapSeverity(diag.code.severity)
					val presentation = EnglishLspDiagnosticLocalizer.localize(diag.code)

					val diagnostic = if (supportsRelatedInformation) {
						encodeWithRelatedInformation(tokenRange, lspSeverity, presentation, uri)
					} else {
						encodeWithMergedMessage(tokenRange, lspSeverity, presentation)
					}

					lspDiagnostics.add(diagnostic)
				}
				currentAbsoluteOffset += event.width
			}
		}
		return lspDiagnostics
	}

	private fun encodeWithRelatedInformation(
		tokenRange: Range,
		lspSeverity: LspSeverity,
		presentation: DiagnosticPresentation,
		uri: String
	): Diagnostic {
		val diagnostic = Diagnostic(tokenRange, presentation.message).apply {
			severity = lspSeverity
			source = "zigocracy"
		}

		presentation.note?.let { noteText ->
			diagnostic.relatedInformation = listOf(
				DiagnosticRelatedInformation(Location(uri, tokenRange), noteText)
			)
		}

		return diagnostic
	}

	private fun encodeWithMergedMessage(
		tokenRange: Range,
		lspSeverity: LspSeverity,
		presentation: DiagnosticPresentation
	): Diagnostic {
		val formattedMessage = buildString {
			append(presentation.message)
			presentation.note?.let { noteText ->
				append(".")
				appendLine()
				append("Note: ")
				append(noteText)
			}
		}

		return Diagnostic(tokenRange, formattedMessage).apply {
			severity = lspSeverity
			source = "zigocracy"
		}
	}
}

private fun mapSeverity(severity: ZigSeverity): LspSeverity = when (severity) {
	ZigSeverity.Error -> LspSeverity.Error
	ZigSeverity.Warning -> LspSeverity.Warning
}