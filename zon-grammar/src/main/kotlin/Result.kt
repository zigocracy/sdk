package net.landless_city.zigocracy.zon

/**
 * A position in source text — computed from byte offset.
 */
public data class SourceLocation(
	val offset: Int,
	val line: Int,
	val column: Int,
)

/**
 * A structured diagnostic message with source context.
 */
public data class Diagnostic(
	val message: String,
	val location: SourceLocation,
	val sourceLine: String,
)

// ── Lexer result ───────────────────────────────────────────────────────

public sealed interface LexResult {
	public data class Success(val tokens: List<Token>) : LexResult
	public data class Error(val diagnostic: Diagnostic) : LexResult
}

// ── Parser result ──────────────────────────────────────────────────────

public sealed interface ParseResult {
	public data class Success(val node: ZonAstNode) : ParseResult
	public data class Error(val diagnostic: Diagnostic) : ParseResult
}

// ── Helpers ────────────────────────────────────────────────────────────

/** Compute line (1-based) and column (1-based) from a byte offset. */
public fun computeLocation(source: String, offset: Int): SourceLocation {
	val line = source.substring(0, offset.coerceAtMost(source.length)).count { it == '\n' } + 1
	val lastNewline = source.lastIndexOf('\n', (offset - 1).coerceAtLeast(0))
	val column = offset - lastNewline
	return SourceLocation(offset = offset, line = line, column = column)
}

/** Extract the line of source text that contains [offset]. */
public fun extractSourceLine(source: String, offset: Int): String {
	val start = source.lastIndexOf('\n', (offset - 1).coerceAtLeast(0)) + 1
	val end = source.indexOf('\n', offset).let { if (it == -1) source.length else it }
	return source.substring(start, end)
}

/** Build a [Diagnostic] for an error at the given [offset]. */
public fun diagnosticAt(
	source: String,
	offset: Int,
	message: String,
): Diagnostic = Diagnostic(
	message = message,
	location = computeLocation(source, offset),
	sourceLine = extractSourceLine(source, offset),
)
