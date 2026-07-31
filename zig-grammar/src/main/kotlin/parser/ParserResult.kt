package net.landless_city.zigocracy.zig.parser

import net.landless_city.zigocracy.zig.shared.CodeUnits
import net.landless_city.zigocracy.zig.shared.DiagnosticCode
import net.landless_city.zigocracy.zig.shared.SourceFile
import net.landless_city.zigocracy.zig.syntax.SyntaxStream

/**
 * The final, indivisible snapshot of a source file's entire structure and faults.
 *
 * This aggregate acts as the definitive data model for the editor. Instead of passing
 * the text, the token structure, and the error list as floating, independent arrays—which
 * easily causes indices to drift out of sync when code changes—this structure permanently
 * locks them together.
 *
 * It guarantees that any position index found in the token stream or the diagnostic list
 * can be instantly and safely cross-referenced back to the physical characters of the original text.
 */
@JvmRecord
data class ParserResult(
	val source: SourceFile,
	val stream: SyntaxStream,
	val diagnostics: List<SyntaxDiagnostic>
)

/**
 * A flattened, source-relative diagnostic entry bound to absolute file coordinates.
 *
 * Examples of coordinate projection from relative token space to absolute file space:
 * - A `MalformedHexEscape` error discovered at relative index 2 inside a string token located
 *   at cursor offset 10 will yield an absolute `startPosition = 12`.
 * - An `AmbiguousCommentStyle` warning at file start will map directly to `startPosition = 0`.
 */
@JvmRecord
data class SyntaxDiagnostic(
	val code: DiagnosticCode,
	val startPosition: CodeUnits,
	val width: CodeUnits,
) {
	val endPosition: CodeUnits get() = startPosition + width
}