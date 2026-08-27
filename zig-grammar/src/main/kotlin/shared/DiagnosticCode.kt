package com.zigocracy.sdk.zig.shared

sealed interface DiagnosticCode {
	val severity: DiagnosticSeverity

	public enum class NumberError(
		override val severity: DiagnosticSeverity = DiagnosticSeverity.Error
	) : DiagnosticCode {
		LeadingZero(DiagnosticSeverity.Error),
		MissingDigitAfterBase(DiagnosticSeverity.Error),
		InvalidDigit(DiagnosticSeverity.Error),
		MalformedUnderscore(DiagnosticSeverity.Error),
		MultipleDecimalPoints(DiagnosticSeverity.Error),
		MissingExponentDigits(DiagnosticSeverity.Error);
	}

	public enum class StringError(
		override val severity: DiagnosticSeverity = DiagnosticSeverity.Error
	) : DiagnosticCode {
		UnterminatedString,
		UnknownEscapeSequence,
		MalformedHexEscape,
		MalformedUnicodeEscape;
	}

	public enum class CharError(
		override val severity: DiagnosticSeverity = DiagnosticSeverity.Error
	) : DiagnosticCode {
		UnterminatedChar,
		EmptyCharLiteral,
		OverlongCharLiteral;
	}

	public enum class CommentError(
		override val severity: DiagnosticSeverity = DiagnosticSeverity.Error
	) : DiagnosticCode {
		AmbiguousCommentStyle(DiagnosticSeverity.Warning);
	}
}
