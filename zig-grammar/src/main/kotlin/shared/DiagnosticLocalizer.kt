package net.landless_city.zigocracy.zig.shared

@JvmRecord
data class DiagnosticPresentation(
	val message: String,
	val note: String? = null
)

interface DiagnosticLocalizer {
	fun localize(code: DiagnosticCode): DiagnosticPresentation
}

infix fun String.withNote(note: String): DiagnosticPresentation = DiagnosticPresentation(this, note)

object EnglishLspDiagnosticLocalizer : DiagnosticLocalizer {
	override fun localize(code: DiagnosticCode): DiagnosticPresentation = when (code) {
		is DiagnosticCode.StringError -> when (code) {
			DiagnosticCode.StringError.UnterminatedString -> "Unterminated string" withNote "Missing a closing double quote (\")."
			DiagnosticCode.StringError.UnknownEscapeSequence -> "Invalid escape sequence" withNote "The sequence following the backslash is not recognized."
			DiagnosticCode.StringError.MalformedHexEscape -> "Malformed hex escape" withNote "Hex escape (\\x) requires exactly 2 valid hexadecimal digits."
			DiagnosticCode.StringError.MalformedUnicodeEscape -> "Malformed Unicode escape" withNote "Expected 1 to 6 hex digits enclosed in curly braces: \\u{...}"
		}

		is DiagnosticCode.CharError -> when (code) {
			DiagnosticCode.CharError.UnterminatedChar -> "Unterminated character literal" withNote "Missing a closing single quote (')."
			DiagnosticCode.CharError.EmptyCharLiteral -> "Empty character literal" withNote "Empty character literals are not supported."
			DiagnosticCode.CharError.OverlongCharLiteral -> "Overlong character literal" withNote "Character literals must represent a single codepoint. Use double quotes (\"...\") for multi-character strings."
		}

		is DiagnosticCode.CommentError -> when (code) {
			DiagnosticCode.CommentError.AmbiguousCommentStyle -> "Ambiguous comment" withNote "Three slashes (///) denote a doc-comment. Adding more slashes (e.g., ////) downgrades it to a standard comment. Insert a space if a visual divider was intended."
		}

		is DiagnosticCode.NumberError -> when (code) {
			DiagnosticCode.NumberError.LeadingZero -> "Invalid octal literal" withNote "C-style octal literals with a leading zero (e.g., 077) are illegal in Zig. Use the '0o' prefix instead (e.g., 0o77)."
			DiagnosticCode.NumberError.MissingDigitAfterBase -> "Missing digits after base" withNote "Radix prefix must be followed by numerical digits (e.g., 0x1, 0b1, 0o1)."
			DiagnosticCode.NumberError.InvalidDigit -> "Invalid digit for numeric base" withNote "Ensure digits match the prefix scope (e.g., only 0-7 for '0o', 0-1 for '0b', or 0-F for '0x')."
			DiagnosticCode.NumberError.MalformedUnderscore -> "Malformed underscore" withNote "Underscores cannot be at the trailing edge or adjacent to a decimal point."
			DiagnosticCode.NumberError.MultipleDecimalPoints -> "Multiple decimal points" withNote "A numeric literal can only have one decimal point (e.g., 3.14)."
			DiagnosticCode.NumberError.MissingExponentDigits -> "Missing exponent digits" withNote "The exponential marker is missing its scale digits (e.g., 1e5, 0x1p2)."
		}
	}
}