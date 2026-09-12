package com.zigocracy.sdk.engine.diagnostics

/**
 * Diagnostic codes emitted by the engine semantic and workspace analysis layers.
 *
 * Distinct bounded context from grammar/lexer diagnostic codes.
 */
sealed interface DiagnosticCode {
	val severity: DiagnosticSeverity

	enum class Import(
		override val severity: DiagnosticSeverity = DiagnosticSeverity.Error
	) : DiagnosticCode {
		FileNotFound(DiagnosticSeverity.Error),
		FileOutsideModule(DiagnosticSeverity.Error),
		InvalidPath(DiagnosticSeverity.Error),
		ModuleNotFound(DiagnosticSeverity.Error),
		UnresolvableContextPath(DiagnosticSeverity.Error);
	}
}

enum class DiagnosticSeverity {
	Error,
	Warning,
	Information,
	Hint;
}
