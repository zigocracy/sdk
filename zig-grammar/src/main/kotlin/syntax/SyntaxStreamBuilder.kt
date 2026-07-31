package net.landless_city.zigocracy.zig.syntax

import net.landless_city.zigocracy.zig.text.CodeUnits

/**
 * Provides a sequential append-only builder for constructing a [SyntaxStream].
 *
 * This builder interleaves two distinct delineations within a single flat sequence:
 * a physical stream of terminal tokens and a logical stream of structural nodes.
 * Hierarchical relationships remain implicit, determined by post-order boundaries.
 */
class SyntaxStreamBuilder {
	private val events: MutableList<SyntaxEvent> = mutableListOf()

	/**
	 * Records a baseline coordinate marking the origin of a future syntax node.
	 *
	 * Call this method before parsing a new grammatical rule. The returned [StartMark]
	 * establishes the exact starting boundary required later by [emitNode] to wrap
	 * the consumed sequence.
	 */
	fun recordStart(): StartMark {
		return StartMark(startIndex = events.size)
	}

	/**
	 * Appends a terminal token to the syntax event sequence.
	 *
	 * Call this method when the parser consumes a language symbol, such as a
	 * keyword, operator, or identifier, as well as any interleaved syntax trivia.
	 * The added token becomes part of the grammatical element being built.
	 */
	fun addToken(kind: TokenKind, width: CodeUnits) {
		events.add(TokenEvent(kind, width))
	}

	/**
	 * Consolidates all events added since a checkpoint into a finished syntax node.
	 *
	 * Call this method when a grammatical rule is successfully matched. This groups
	 * all tokens and nested sub-nodes added since the specified [mark] was created
	 * into a single element of the given [kind].
	 */
	fun emitNode(mark: StartMark, kind: NodeKind) {
		val childCount = events.size - mark.startIndex
		events.add(NodeEvent(kind, childCount))
	}

	fun build() = SyntaxStream(events)

	/**
	 * A checkpoint representing the starting coordinate of a structural
	 * node within the flat event sequence.
	 */
	@JvmInline
	value class StartMark(val startIndex: Int)
}