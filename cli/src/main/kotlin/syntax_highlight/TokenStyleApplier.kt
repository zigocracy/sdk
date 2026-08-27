package com.zigocracy.sdk.cli.syntax_highlight

import com.zigocracy.sdk.zig.lexer.TokenDiagnostic
import com.zigocracy.sdk.zig.syntax.TokenEvent

internal object TokenStyleApplier {
	fun applyStyles(
		rawText: String,
		event: TokenEvent,
		diagnostics: List<TokenDiagnostic>,
		theme: SyntaxHighlightTheme
	): String {
		val baseStyle = theme.getSyntaxStyle(event.kind)
		if (diagnostics.isEmpty()) {
			return baseStyle(rawText)
		}

		// Get diagnostics that point to more precise parts of token
		val visualDiagnostics = diagnostics.sortedBy { it.startOffset }
		val primaryDiagnostic = diagnostics.minBy { it.code.severity }
		val errorTokenStyle = baseStyle + theme.getDiagnosticUnderline(primaryDiagnostic.code)

		return buildString {
			var cursor = 0
			for (diag in visualDiagnostics) {
				if (diag.startOffset > cursor) {
					val leftPart = rawText.substring(cursor, diag.startOffset)
					append(errorTokenStyle(leftPart))
					cursor = diag.startOffset
				}

				val errorPartStyle = theme.getDiagnosticUnderline(primaryDiagnostic.code) +
					theme.getDiagnosticBackgroundHighlight(diag.code)

				if (diag.width == 0) {
					val targetPos = diag.startOffset.coerceAtLeast(cursor)
					if (targetPos < rawText.length) {
						val errorPart = rawText.substring(targetPos, targetPos + 1)
						append(errorPartStyle(errorPart))
						cursor = targetPos + 1
					} else {
						append(errorPartStyle(" "))
						cursor = rawText.length
					}
				} else {
					val end = diag.endOffset.coerceIn(cursor, rawText.length)
					if (end > cursor) {
						val errorPart = rawText.substring(cursor, end)
						append(errorPartStyle(errorPart))
						cursor = end
					}
				}
			}

			if (cursor < rawText.length) {
				val rightPart = rawText.substring(cursor)
				append(errorTokenStyle(rightPart))
			}
		}
	}
}