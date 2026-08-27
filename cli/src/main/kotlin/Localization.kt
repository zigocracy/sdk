package net.landless_city.zigocracy.zig.cli

import net.landless_city.zigocracy.zig.shared.DiagnosticCode

internal interface DiagnosticLocalizer {
	fun getMessage(code: DiagnosticCode): String
	fun getNote(code: DiagnosticCode): String?
}

internal object EnglishDiagnosticLocalizer : DiagnosticLocalizer {
	override fun getMessage(code: DiagnosticCode): String = when (code) {
		is DiagnosticCode.StringError -> when (code) {
			DiagnosticCode.StringError.UnterminatedString -> "unterminated string literal"
			DiagnosticCode.StringError.UnknownEscapeSequence -> "unknown escape sequence"
			DiagnosticCode.StringError.MalformedHexEscape -> "hex escape requires exactly two hex digits"
			DiagnosticCode.StringError.MalformedUnicodeEscape -> "malformed unicode escape"
		}

		is DiagnosticCode.CharError -> when (code) {
			DiagnosticCode.CharError.UnterminatedChar -> "unterminated character literal"
			DiagnosticCode.CharError.EmptyCharLiteral -> "empty character literal is illegal"
			DiagnosticCode.CharError.OverlongCharLiteral -> "character literal contains too many characters"
		}

		is DiagnosticCode.CommentError -> when (code) {
			DiagnosticCode.CommentError.AmbiguousCommentStyle -> "ambiguous comment style"
		}

		is DiagnosticCode.NumberError -> when (code) {
			DiagnosticCode.NumberError.LeadingZero -> "leading zeros in integer literals are illegal"
			DiagnosticCode.NumberError.MissingDigitAfterBase -> "missing digit after radix prefix"
			DiagnosticCode.NumberError.InvalidDigit -> "invalid digit for numeric base"
			DiagnosticCode.NumberError.MalformedUnderscore -> "invalid underscore placement in numeric literal"
			DiagnosticCode.NumberError.MultipleDecimalPoints -> "multiple decimal points in numeric literal"
			DiagnosticCode.NumberError.MissingExponentDigits -> "missing exponent digits after exponent marker"
		}
	}

	override fun getNote(code: DiagnosticCode): String? = when (code) {
		DiagnosticCode.StringError.MalformedUnicodeEscape -> "expected 1 to 6 hex digits inside curly braces, e.g., \\u{1f408}"
		DiagnosticCode.NumberError.LeadingZero -> "use the '0o' prefix for octal literals, e.g., 0o77"
		DiagnosticCode.NumberError.MissingDigitAfterBase -> "radix prefix requires numerical digits following it, e.g., 0x1, 0b1, 0o1"
		DiagnosticCode.CommentError.AmbiguousCommentStyle -> "interpreted as regular comment; insert a space if a visual divider was intended"
		else -> null
	}
}