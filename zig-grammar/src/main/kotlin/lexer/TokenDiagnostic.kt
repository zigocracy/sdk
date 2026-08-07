package net.landless_city.zigocracy.zig.lexer

import net.landless_city.zigocracy.zig.shared.DiagnosticCode
import net.landless_city.zigocracy.zig.text.CodeUnits

/**
 * An error or warning found inside a single token.
 *
 * @param code The identifier and severity of the diagnostic issue.
 * @param startOffset The position of the issue, counted from the start of the token.
 * @param width The physical length of the problem sequence in the text.
 */
@JvmRecord
data class TokenDiagnostic(
	val code: DiagnosticCode,
	val startOffset: CodeUnits,
	val width: CodeUnits
) {
	/**
	 * The end position of the issue, counted from the start of the token.
	 */
	val endOffset: CodeUnits get() = startOffset + width
}