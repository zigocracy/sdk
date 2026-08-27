package net.landless_city.zigocracy.zig.syntax

import net.landless_city.zigocracy.zig.lexer.TokenDiagnostic

/**
 * A visitor for processing a syntax stream in its logical order.
 *
 * Traversal flows hierarchically from top to bottom (nodes before their children),
 *  and sequentially from left to right as they appear in the source code.
 */
interface SyntaxStreamVisitor {
	/**
	 * Called when a terminal token is reached.
	 *
	 * Position offsets in the provided [diagnostics] are measured from the start of this token.
	 */
	fun visitToken(index: Int, event: TokenEvent, diagnostics: List<TokenDiagnostic>) {}

	/**
	 * Called when entering a syntax node, before its children are processed.
	 *
	 * @return true to process the children of this node. Return false to skip this node's
	 * entire subtree and move directly to its next sibling.
	 */
	fun enterNode(index: Int, event: NodeEvent): Boolean

	/**
	 * Called when leaving a syntax node after all its children are processed.
	 */
	fun leaveNode(index: Int, event: NodeEvent) {}
}


/**
 * Traverses the syntax stream from the root node down to its leaves using a depth-first strategy.
 *
 * This function converts the flat post-order event list into a hierarchical tree order,
 * calling the appropriate methods on the [visitor] for each node and token.
 *
 * @param visitor The visitor that processes each element during the traversal.
 */
fun SyntaxStream.traverseFromRoot(visitor: SyntaxStreamVisitor) {
	if (events.isEmpty()) return
	visitElementAt(events.lastIndex, visitor)
}

private fun SyntaxStream.visitElementAt(index: Int, visitor: SyntaxStreamVisitor) {
	when (val event = events[index]) {
		is TokenEvent -> {
			val tokenDiagnostics = diagnostics.getOrDefault(index, emptyList())
			visitor.visitToken(index, event, tokenDiagnostics)
		}

		is NodeEvent -> {
			if (visitor.enterNode(index, event)) {
				visitChildren(index, event, visitor)
			}
			visitor.leaveNode(index, event)
		}
	}
}

private fun SyntaxStream.visitChildren(parentIndex: Int, event: NodeEvent, visitor: SyntaxStreamVisitor) {
	val childrenSlots = event.childCount
	if (childrenSlots <= 0) return

	val startIndex = parentIndex - childrenSlots
	var currentIndex = startIndex

	while (currentIndex < parentIndex) {
		visitElementAt(currentIndex, visitor)

		val childEvent = events[currentIndex]
		val innerSlots = if (childEvent is NodeEvent) childEvent.childCount else 0

		// Advance to the next sibling on the same level.
		// We add 1 for the child itself + the slots of its already processed descendants.
		currentIndex += 1 + innerSlots
	}
}