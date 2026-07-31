package net.landless_city.zigocracy.zig.syntax

import net.landless_city.zigocracy.zig.lexer.TokenDiagnostic
import net.landless_city.zigocracy.zig.text.CodeUnits

/**
 * A high-fidelity structural snapshot of a parsed document.
 *
 * This stream flattens the code hierarchy into a single continuous sequence, serving
 * as the foundational contract for all downstream syntax analysis.
 *
 * ### Data Fidelity
 *
 * The stream preserves every textual detail, symbol category, and formatting artifact
 * without information loss. Because the raw document context is fully retained, you have
 * total flexibility to choose exactly how much detail your task requires.
 *
 * You can selectively map individual sections, query tokens sequentially, or convert the
 * data into custom tree representations. For example:
 * - A Concrete Syntax Tree can utilize the full stream to capture every whitespace and comment.
 * - An Abstract Syntax Tree can skip layout trivia to focus strictly on programmatic logic.
 * - Quick diagnostics or line-of-code counters can look at raw tokens directly
 *
 * ### Structure & Order
 *
 * The stream encodes the tree hierarchy implicitly using a strict post-order layout.
 * Within this single sequence, terminal tokens act as leaves and flow in their natural,
 * left-to-right reading order. Structural nodes act as parent containers that wrap around
 * these leaves.
 */
class SyntaxStream(
	val events: List<SyntaxEvent>,
	val diagnostics: Map<Int, List<TokenDiagnostic>>,
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
	 *
	 * Time Complexity:
	 * - Best Case: Θ(1) if the event is a [TokenEvent].
	 * - Worst Case: Θ(K) if the event is a [NodeEvent], where K is [NodeEvent.childCount].
	 *
	 * Memory Complexity: Θ(1)
	 */
	fun computeWidthAt(index: Int): CodeUnits = when (val event = events[index]) {
		is TokenEvent -> event.width
		is NodeEvent -> childrenOf(index, event).sumTokenWidths()
	}

	/**
	 * Computes the total text width of the entire syntax stream.
	 *
	 * Time Complexity: Θ(N), where N is the total number of events in the stream.
	 *
	 * Memory Complexity: Θ(1)
	 */
	fun computeTotalWidth(): CodeUnits = events.sumTokenWidths()
}

/**
 * Computes the total text width of this syntax event sequence.
 *
 * Time Complexity: Θ(N), where N is the number of events in the sequence.
 *
 * Memory Complexity: Θ(1)
 */
@Suppress("NOTHING_TO_INLINE")
private inline fun List<SyntaxEvent>.sumTokenWidths(): CodeUnits =
	sumOf { event -> if (event is TokenEvent) event.width else 0 }