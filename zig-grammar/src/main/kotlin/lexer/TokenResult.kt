package com.zigocracy.sdk.zig.lexer

import com.zigocracy.sdk.zig.shared.DiagnosticSeverity
import com.zigocracy.sdk.zig.syntax.TokenEvent


/**
 * The result of reading a single token from the text.
 *
 * @param event The layout information of the recognized token (its kind and width).
 * @param diagnostics The list of local errors or warnings found inside this token.
 */
@JvmRecord
data class TokenResult(
	val event: TokenEvent,
	val diagnostics: List<TokenDiagnostic>
) {
	/**
	 * True if the token has critical errors that make it invalid.
	 *
	 * Warnings or style issues do not break the token validity.
	 */
	val isMalformed: Boolean
		get() = diagnostics.any { it.code.severity == DiagnosticSeverity.Error }
}