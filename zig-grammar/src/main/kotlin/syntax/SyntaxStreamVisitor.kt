package com.zigocracy.sdk.zig.syntax

import com.zigocracy.sdk.zig.lexer.TokenDiagnostic

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
 * Traverses the syntax stream starting from the root node in depth-first source order.
 *
 * Each node is visited via [SyntaxStreamVisitor.enterNode] before traversing its children,
 * followed by [SyntaxStreamVisitor.leaveNode] once all descendant nodes and tokens have
 * been processed. Terminal tokens are delivered to [SyntaxStreamVisitor.visitToken] in
 * their source order with any attached diagnostics.
 *
 * If [SyntaxStreamVisitor.enterNode] returns `false`, the subtree is pruned and
 * traversal resumes at the next sibling.
 *
 * @param visitor The visitor that processes each node and token during traversal.
 */
fun SyntaxStream.traverseFromRoot(visitor: SyntaxStreamVisitor) {
	if (events.isEmpty()) return
	val cursor = cursor()

	while (true) {
		val index = cursor.index
		when (val event = cursor.event) {
			is TokenEvent -> {
				val tokenDiagnostics = diagnostics.getOrDefault(index, emptyList())
				visitor.visitToken(index, event, tokenDiagnostics)

				if (!cursor.forward()) {
					if (!cursor.ascendAndLeave(visitor)) break
				}
			}

			is NodeEvent -> {
				val shouldDescend = visitor.enterNode(index, event)
				if (shouldDescend && cursor.down()) {
					continue
				} else {
					visitor.leaveNode(index, event)
					if (!cursor.forward()) {
						if (!cursor.ascendAndLeave(visitor)) break
					}
				}
			}
		}
	}
}

private fun SyntaxStreamCursor.ascendAndLeave(visitor: SyntaxStreamVisitor): Boolean {
	while (depth > 0) {
		up()
		visitor.leaveNode(index, event as NodeEvent)
		if (forward()) return true
	}
	return false
}