package net.landless_city.zigocracy.zig.scanner

import net.landless_city.zigocracy.zig.shared.CodeUnits
import net.landless_city.zigocracy.zig.shared.DiagnosticCode
import net.landless_city.zigocracy.zig.syntax.TokenKind

/**
 * The outcome of a single token scan operation.
 */
sealed interface ScanResult {
	/**
	 * Indicates the upcoming text sequence does not match this scanner's category.
	 */
	object NoMatch : ScanResult

	/**
	 * Represents a successful lexical match with the text.
	 *
	 * @property kind The classified grammatical category of the recognized token.
	 * @property width The total physical length of the token in code units, including any
	 *                 malformed trailing sequences consumed for error resilience.
	 * @property diagnostics Collection of fine-grained syntax errors or warnings discovered
	 *                       internally.
	 */
	@JvmRecord
	data class Matched(
		val kind: TokenKind,
		val width: CodeUnits,
		val diagnostics: List<TokenDiagnostic>
	) : ScanResult
}

/**
 * A local syntax failure captured during a single token scan.
 *
 * These coordinates are anchored relative to the starting character of this specific token,
 * allowing individual scanners to pinpoint errors without needing absolute file positions.
 */
@JvmRecord
data class TokenDiagnostic(
	val code: DiagnosticCode,
	val relativeStart: CodeUnits,
	val width: CodeUnits,
) {
	val relativeEnd: CodeUnits get() = relativeStart + width
}