package com.zigocracy.sdk.engine.diagnostics

/**
 * A diagnostic issue identified during engine analysis.
 *
 * @property code The category and code of the diagnostic.
 * @property startOffset Absolute character offset from the beginning of the file.
 * @property width Length of the diagnosed range in code units.
 * @property message User-facing explanatory message.
 * @property note Optional supplementary hint or note.
 */
data class EngineDiagnostic(
	val code: DiagnosticCode,
	val startOffset: Int,
	val width: Int,
	val message: String,
	val note: String? = null,
)
