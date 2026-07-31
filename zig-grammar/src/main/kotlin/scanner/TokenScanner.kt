package net.landless_city.zigocracy.zig.scanner

/**
 * A reusable parsing component responsible for isolating and sizing a single token kind.
 *
 * Implement this interface to add new grammatical categories to the syntax analyzer.
 * The implementer must guarantee a stateless, read-only inspection of the provided input,
 * returning either a successful classification with a non-zero width or a refusal to match.
 */
interface TokenScanner {
	/**
	 * Inspects the source text via [reader] to categorize and size the upcoming sequence.
	 *
	 * - **On Failure**: Returns [ScanResult.NoMatch] immediately without mutational side effects.
	 * - **On Success**: Returns [ScanResult.Matched] with the precise physical token width.
	 */
	fun scan(reader: TextReader): ScanResult
}