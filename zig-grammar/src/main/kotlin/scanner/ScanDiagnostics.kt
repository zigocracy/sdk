package net.landless_city.zigocracy.zig.scanner

import net.landless_city.zigocracy.zig.shared.DiagnosticCode
import net.landless_city.zigocracy.zig.text.CodeUnits

/**
 * A local syntax failure captured during a single scanner pass.
 *
 * @param code The unique identifier and severity of the failure.
 * @param offset The relative offset from the beginning of the sequence currently being scanned.
 * @param width The physical span length of the malformed text block.
 */
@JvmRecord
internal data class ScanDiagnostics(
	val code: DiagnosticCode,
	val offset: CodeUnits,
	val width: CodeUnits,
)