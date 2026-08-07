package net.landless_city.zigocracy.zig.lexer

import net.landless_city.zigocracy.zig.scanner.ScanDiagnostics
import net.landless_city.zigocracy.zig.scanner.ScanResult
import net.landless_city.zigocracy.zig.scanner.impl.*
import net.landless_city.zigocracy.zig.syntax.TokenEvent
import net.landless_city.zigocracy.zig.syntax.TokenKind
import net.landless_city.zigocracy.zig.text.TextReader

/**
 * A stateless tokenizer that classifies character sequences into lexical tokens.
 */
object Tokenizer {

	/**
	 * Reads and categorizes the next token from the reader.
	 *
	 * @param reader A read-only view of the text positioned at the start of the token.
	 * @return The recognized token result, or `null` if the reader has reached EOF.
	 */
	fun tokenizeNext(reader: TextReader): TokenResult? {
		if (reader.peekChar() == null) return null

		// Internal implementation detail: Multi-scanner priority evaluation pipeline
		for (scanner in scanners) {
			val result = scanner.scan(reader)
			if (result is ScanResult.Matched) {
				val event = TokenEvent(result.kind, result.width)
				val diagnostics = result.diagnostics.map { it.toTokenDiagnostic() }

				return TokenResult(event, diagnostics)
			}
		}

		return TokenResult(ERROR_EVENT, emptyList())
	}

	private fun ScanDiagnostics.toTokenDiagnostic() = TokenDiagnostic(
		this.code,
		this.offset,
		this.width,
	)

	private val ERROR_EVENT = TokenEvent(TokenKind.ErrorToken, width = 1)

	// Must be sorted in order of grammatical domain priority
	private val scanners = listOf(
		WhitespaceScanner,
		CommentScanner,
		StringScanner,
		MultilineStringScanner,
		CharScanner,
		PunctuationScanner,
		BuiltinIdentifierScanner,
		IdentifierScanner,
		NumberScanner,
	)
}