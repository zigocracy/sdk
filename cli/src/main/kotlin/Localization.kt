package net.landless_city.zigocracy.zig.cli

import net.landless_city.zigocracy.zig.shared.DiagnosticCode

internal interface DiagnosticLocalizer {
	fun getMessage(code: DiagnosticCode): String
}

internal object EnglishDiagnosticLocalizer : DiagnosticLocalizer {
	override fun getMessage(code: DiagnosticCode): String = when (code) {
		is DiagnosticCode.StringError -> when (code) {
			DiagnosticCode.StringError.UnterminatedString -> "String literal is missing a closing double quote (\")."
			DiagnosticCode.StringError.UnknownEscapeSequence -> "The sequence following the backslash is not a recognized escape character."
			DiagnosticCode.StringError.MalformedHexEscape -> "Hexadecimal escape sequence (\\x) requires exactly two valid hex digits."
			DiagnosticCode.StringError.MalformedUnicodeEscape -> "Unicode escape sequence (\\u{...}) is malformed. Expected 1 to 6 hexadecimal digits enclosed in curly braces."
		}

		is DiagnosticCode.CharError -> when (code) {
			DiagnosticCode.CharError.UnterminatedChar -> "Character literal is missing a closing single quote (')."
			DiagnosticCode.CharError.EmptyCharLiteral -> "Character literal cannot be empty ('' is illegal in Zig)."
			DiagnosticCode.CharError.OverlongCharLiteral -> "Character literal contains too many characters. It must represent exactly one codepoint."
		}

		is DiagnosticCode.CommentError -> when (code) {
			DiagnosticCode.CommentError.AmbiguousCommentStyle -> "Ambiguous comment style (////). Interpreted as a regular comment. Insert a space if a visual section divider was intended."
		}

		is DiagnosticCode.NumberError -> when (code) {
			DiagnosticCode.NumberError.LeadingZero -> "Octal literals using a leading zero (e.g., 077) are illegal in Zig. Use the '0o' prefix instead (0o77)."
			DiagnosticCode.NumberError.MissingDigitAfterBase -> "Radix prefix requires at least one valid numerical digit following it (e.g., 0x1, 0b1, 0o1)."
			DiagnosticCode.NumberError.InvalidDigit -> "The character is not a valid digit within the current numeric literal base."
			DiagnosticCode.NumberError.MalformedUnderscore -> "Underscores cannot be placed at the trailing edge of a number or directly adjacent to a decimal point boundary."
			DiagnosticCode.NumberError.MultipleDecimalPoints -> "Numeric literal contains multiple fractional decimal points, which is invalid."
			DiagnosticCode.NumberError.MissingExponentDigits -> "The exponential scale marker is missing its following exponent layout scale digits (e.g., 1e5, 0x1p2)."
		}
	}
}