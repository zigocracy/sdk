package net.landless_city.zigocracy.zig.scanner.util

import net.landless_city.zigocracy.zig.scanner.TextReader
import net.landless_city.zigocracy.zig.scanner.TokenDiagnostic
import net.landless_city.zigocracy.zig.shared.CodeUnits
import net.landless_city.zigocracy.zig.shared.DiagnosticCode

object EscapeSequence {

	/**
	 * Universally scans any escape sequence relative to the provided base offset.
	 * Expects the character at [base] to be a backslash.
	 *
	 * @param reader The raw source code text reader.
	 * @param base The absolute code unit offset of the '\\' character.
	 * @param context The recovery strategy containing active synchronizing token boundaries.
	 * @param diagnostics The mutable target list to collect fine-grained syntax errors.
	 * @return The total physical width in code units consumed by this sequence.
	 */
	fun scan(
		reader: TextReader,
		base: CodeUnits,
		context: RecoveryContext,
		diagnostics: MutableList<TokenDiagnostic>
	): CodeUnits {
		if (reader.peekChar(base) != '\\') return 0

		val nextChar = reader.peekChar(base + 1) ?: return 1 // Backslash at EOF

		return when (nextChar) {
			'x' -> scanHexEscape(reader, base, context, diagnostics)
			'u' -> scanUnicodeEscape(reader, base, context, diagnostics)
			else -> {
				if (nextChar.isZigSimpleEscape()) {
					2 // Backslash + valid character
				} else {
					// Stop before a newline boundary to preserve structural whitespace tokenization
					val isNewline = nextChar.isZigVerticalWhitespace()
					val malformedWidth = if (isNewline) 1 else 2

					diagnostics.add(
						TokenDiagnostic(
							code = DiagnosticCode.StringError.UnknownEscapeSequence,
							relativeStart = base,
							width = malformedWidth
						)
					)
					malformedWidth
				}
			}
		}
	}

	private fun scanHexEscape(
		reader: TextReader,
		base: CodeUnits,
		context: RecoveryContext,
		diagnostics: MutableList<TokenDiagnostic>
	): CodeUnits {
		val hexChunk = reader.peekString(offset = base, width = 4)

		if (hexChunk.length == 4) {
			val h1 = hexChunk[2]
			val h2 = hexChunk[3]
			if (h1.isZigHexDigit() && h2.isZigHexDigit()) {
				return 4 // Successful happy path match
			}
		}

		// Fallback panic-mode recovery attempt
		val prefixLen = 2
		var malformedWidth = prefixLen
		val scanLimit = prefixLen + 1

		for (offset in prefixLen..scanLimit) {
			val lookahead = reader.peekChar(base + offset)

			// Stop if we hit EOF or structural context boundary
			if (lookahead == null || lookahead.isSynchronizingFor(context)) {
				break
			}

			// Protect against overconsuming structural whitespace trivia at code boundaries
			if (lookahead.isZigHorizontalWhitespace() && !context.isInteriorWhitespace(reader, base, malformedWidth)) {
				break
			}

			malformedWidth++
		}

		diagnostics.add(
			TokenDiagnostic(
				code = DiagnosticCode.StringError.MalformedHexEscape,
				relativeStart = base,
				width = malformedWidth
			)
		)
		return malformedWidth
	}

	private const val MIN_UNICODE_DIGITS = 1
	private const val MAX_UNICODE_DIGITS = 6

	private const val MAX_MALFORMED_BODY_LIMIT = MAX_UNICODE_DIGITS * 2
	private const val TOTAL_MALFORMED_ESCAPE_LIMIT = "\\u{".length + MAX_MALFORMED_BODY_LIMIT + "}".length

	private fun scanUnicodeEscape(
		reader: TextReader,
		base: CodeUnits,
		context: RecoveryContext,
		diagnostics: MutableList<TokenDiagnostic>
	): CodeUnits {
		val unicodeChunk = reader.peekString(offset = base, width = 10)

		if (unicodeChunk.length >= 5 && unicodeChunk.startsWith("\\u{")) {
			var digitCount = 0
			var offset = 3

			while (offset < unicodeChunk.length && digitCount < MAX_UNICODE_DIGITS) {
				if (!unicodeChunk[offset].isZigHexDigit()) {
					break
				}
				digitCount++
				offset++
			}

			if (digitCount in MIN_UNICODE_DIGITS..MAX_UNICODE_DIGITS &&
				offset < unicodeChunk.length &&
				unicodeChunk[offset] == '}'
			) {
				return offset + 1 // Successful structural unicode match
			}
		}

		var malformedWidth = 2
		while (true) {
			val lookahead = reader.peekChar(base + malformedWidth)
			when {
				lookahead == null || lookahead.isSynchronizingFor(context) -> break

				lookahead == '}' -> {
					malformedWidth++
					break
				}

				lookahead.isZigHorizontalWhitespace() -> {
					// Use poly-morphic inline check to verify if the space chunk belongs to interior literal block
					if (context.isInteriorWhitespace(reader, base, malformedWidth)) {
						malformedWidth++ // Legal internal whitespace, safe to accumulate
					} else {
						break // Safe termination right before the trailing whitespaces
					}
				}

				else -> {
					malformedWidth++
					if (malformedWidth >= TOTAL_MALFORMED_ESCAPE_LIMIT) {
						break
					}
				}
			}
		}

		diagnostics.add(
			TokenDiagnostic(
				code = DiagnosticCode.StringError.MalformedUnicodeEscape,
				relativeStart = base,
				width = malformedWidth
			)
		)
		return malformedWidth
	}
}