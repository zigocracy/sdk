package net.landless_city.zigocracy.zig.lexer

import net.landless_city.zigocracy.zig.text.TextStream

/**
 * An LL(k) buffer that provides token lookahead capabilities over a text stream.
 */
class LookaheadTokenReader(
	private val stream: TextStream,
	private val maxLookahead: Int = 10,
) {
	init {
		require(maxLookahead > 0) { "Maximum lookahead depth must be positive. Given: $maxLookahead" }
	}

	private val buffer = ArrayDeque<TokenResult>(initialCapacity = maxLookahead + 1)

	/**
	 * Looks ahead into the token stream without consuming the current position.
	 *
	 * Multiple calls with the same distance return the same cached result.
	 *
	 * @param distance The lookahead depth (0 for current token, 1 for next, etc.).
	 * @return The token result snapshot, or `null` if the stream hits EOF at or before the requested distance.
	 */
	fun peek(distance: Int = 0): TokenResult? {
		require(distance >= 0) { "Lookahead distance cannot be negative. Attempted: $distance" }
		require(distance <= maxLookahead) {
			"Lookahead distance $distance exceeds the configured maximum depth of $maxLookahead."
		}

		while (buffer.size <= distance) {
			val nextResult = fetchNextResult() ?: break
			buffer.addLast(nextResult)
		}

		if (distance >= buffer.size) return null

		return buffer[distance]
	}

	/**
	 * Consumes and returns the current token result, advancing the reader window forward.
	 */
	fun consume(): TokenResult? {
		if (buffer.isEmpty()) {
			val nextResult = fetchNextResult() ?: return null
			buffer.addLast(nextResult)
		}
		return buffer.removeFirst()
	}

	private fun fetchNextResult(): TokenResult? {
		val result = Tokenizer.tokenizeNext(stream) ?: return null
		stream.advance(result.event.width)
		return result
	}
}