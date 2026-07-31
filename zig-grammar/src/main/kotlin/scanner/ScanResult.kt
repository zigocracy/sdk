package net.landless_city.zigocracy.zig.scanner

import net.landless_city.zigocracy.zig.syntax.TokenKind
import net.landless_city.zigocracy.zig.text.CodeUnits

/**
 * The internal outcome of a low-level token scan operation.
 */
internal sealed interface ScanResult {
	/**
	 * Indicates the upcoming text sequence does not match this scanner's category.
	 */
	object NoMatch : ScanResult

	/**
	 * Represents a successful lexical match with the text.
	 *
	 * @param kind The grammatical category of the recognized token.
	 * @param width The physical length of the consumed characters.
	 * @param diagnostics Syntax errors or warnings discovered internally during the scan.
	 */
	@JvmRecord
	data class Matched(
		val kind: TokenKind,
		val width: CodeUnits,
		val diagnostics: List<ScanDiagnostics>
	) : ScanResult
}