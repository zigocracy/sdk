package net.landless_city.zigocracy.zig.scanner

import net.landless_city.zigocracy.zig.text.TextReader

/**
 * Internal contract for single-token lexical analyzers.
 *
 * ### Implementor Requirements
 * Must be strictly stateless. Any lookahead state or temporary buffers must be fully
 * encapsulated within the lifecycle of a single [scan] function call.
 */
internal interface Scanner {
	/**
	 * Categorizes and sizes the upcoming character sequence.
	 *
	 * ### Caller Requirements
	 * Do not invoke this method if the [reader] is exhausted. The caller must guarantee
	 * that `reader.peekChar(0) != null` before invocation.
	 *
	 * @param reader A lookahead stream positioned at the first character of the token.
	 * @return [ScanResult.Matched] if recognized, or [ScanResult.NoMatch] if this scanner
	 *         refuses to match the upcoming sequence.
	 */
	fun scan(reader: TextReader): ScanResult
}