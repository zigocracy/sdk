package net.landless_city.zigocracy.zig

class SyntaxStream(
	val sourceFile: SourceFile,
	val events: List<SyntaxEvent>,
) {
	init {
		require(events.isNotEmpty()) {
			"Syntax stream cannot be empty."
		}

		val rootEvent = events.last()
		require(rootEvent is NodeEvent) {
			"The final event in the stream must be a structural root node."
		}

		val expectedChildCount = events.size - 1
		require(rootEvent.childCount == expectedChildCount) {
			"The root node must encapsulate all events in the stream. Expected childCount: $expectedChildCount, but got: ${rootEvent.childCount}."
		}
	}

	private fun childrenOf(nodeIndex: Int, node: NodeEvent): List<SyntaxEvent> {
		val startIndex = nodeIndex - node.childCount
		require(startIndex >= 0) {
			"Invalid node slice boundaries. Computed startIndex ($startIndex) cannot be negative for node at index $nodeIndex with childCount ${node.childCount}."
		}
		return events.subList(startIndex, nodeIndex)
	}

	/**
	 * Computes the total text width of a syntax event at the given [index].
	 */
	fun computeWidthAt(index: Int): CodeUnits = when (val event = events[index]) {
		is TokenEvent -> event.width
		is NodeEvent -> childrenOf(index, event)
			.sumOf { child -> if (child is TokenEvent) child.width else 0 }
	}

	/**
	 * Provides a sequential append-only builder for constructing a [SyntaxStream].
	 *
	 * This builder interleaves two distinct delineations within a single flat sequence:
	 * a physical stream of terminal tokens and a logical stream of structural nodes.
	 * Hierarchical relationships remain implicit, determined by post-order boundaries.
	 */
	internal class Builder(
		val sourceFile: SourceFile,
	) {
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

		fun build() = SyntaxStream(sourceFile, events)

		/**
		 * A checkpoint representing the starting coordinate of a structural
		 * node within the flat event sequence.
		 */
		@JvmInline
		value class StartMark(val startIndex: Int)
	}
}

sealed interface SyntaxEvent {
	val kind: SyntaxKind
}

class TokenEvent(
	override val kind: TokenKind,
	val width: CodeUnits,
) : SyntaxEvent

class NodeEvent(
	override val kind: NodeKind,
	val childCount: Int,
) : SyntaxEvent {
	init {
		require(childCount >= 0)
	}
}