package com.zigocracy.sdk.zig.syntax

/**
 * A stateful cursor for navigating a [SyntaxStream] as a hierarchical tree.
 *
 * A [SyntaxStream] is a container that stores tokens and node boundaries as a flat sequence of
 * events. While this flat representation is great for storage and serialization, navigating code
 * directly by raw event indices is awkward and requires tedious offset math.
 *
 * This cursor provides a convenient tree view over the stream. It allows you to move between
 * parents, children, and siblings without having to build tree objects or manage manual index offsets:
 *
 * ```kotlin
 * val cursor = stream.cursor()
 *
 * cursor.forEachChild { child ->
 *     when (val event = child.event) {
 *         is NodeEvent  -> println("Node: ${event.kind}")
 *         is TokenEvent -> println("Token: ${event.kind}")
 *     }
 * }
 * ```
 *
 * ### Navigation
 * The cursor tracks an active position and moves along two directions:
 * - **Vertical ([down], [up]):** moves between a node and its children.
 * - **Horizontal ([forward], [backward]):** steps between sibling nodes at the same level.
 *
 * ### Direct Children and Descendants
 * In a syntax stream, a node wraps all the events that belong to it. These events fall into
 * two categories:
 * - **Direct children:** the parts that immediately make up the node.
 * - **Descendants:** any code nested deeper inside those parts.
 *
 * The cursor always moves between direct children. Calling [down] enters the first direct child,
 * and calling [forward] steps to the next direct child on the same level, jumping cleanly over
 * any nested code inside.
 *
 * For example, in a function declaration:
 * - Its direct children are things like the `fn` keyword, function name, parameter list, return type,
 *   and body block.
 * - The statements and expressions *inside* that body block are descendants of the function,
 *   but they are direct children only of the block itself.
 *
 * ### Boundaries and State
 * Every navigation method returns a boolean indicating whether the move succeeded:
 * - Returns `true` if the cursor successfully moved to the target element.
 * - Returns `false` if movement is not possible (for example, calling [up] at the root node,
 *   or calling [forward] on the last sibling). When a method returns `false`, the cursor stays
 *   at its current position.
 *
 * The root node starts at depth `0`. Stepping [down] increments depth by 1, and stepping [up] decrements it.
 *
 * @see SyntaxStream
 */
class SyntaxStreamCursor internal constructor(
	val stream: SyntaxStream,
	private val initialIndex: Int = stream.events.lastIndex,
) {
	init {
		require(initialIndex in stream.events.indices) {
			"Initial cursor index $initialIndex is out of bounds [0, ${stream.events.size})."
		}
	}

	private var path = IntArray(16).also { it[0] = initialIndex }

	/**
	 * The current nesting depth from the root node (root is at depth 0).
	 */
	var depth: Int = 0
		private set

	/**
	 * The index of the current syntax event in [SyntaxStream.events].
	 */
	val index: Int
		get() = path[depth]

	/**
	 * The current [SyntaxEvent].
	 */
	val event: SyntaxEvent
		get() = stream.events[index]

	/**
	 * Moves to the first direct child of the current node.
	 *
	 * @return `true` if the cursor moved to the first child;
	 *         `false` if the current element is a token, an empty node, or cannot be entered
	 *         (the cursor position remains unchanged).
	 */
	fun down(): Boolean {
		val curr = index
		val currEvent = stream.events[curr]
		if (currEvent !is NodeEvent || currEvent.childCount == 0) return false

		val firstChildStart = curr - currEvent.childCount
		var child = curr - 1
		while (true) {
			val ev = stream.events[child]
			val slotCount = if (ev is NodeEvent) 1 + ev.childCount else 1
			val prev = child - slotCount
			if (prev < firstChildStart) break
			child = prev
		}

		val nextDepth = depth + 1
		ensureCapacity(nextDepth + 1)
		depth = nextDepth
		path[depth] = child
		return true
	}

	/**
	 * Moves to the parent of the current element.
	 *
	 * @return `true` if the cursor moved to the parent node;
	 *         `false` if the cursor is already at the root (the cursor position remains unchanged).
	 */
	fun up(): Boolean {
		if (depth == 0) return false
		depth--
		return true
	}

	/**
	 * Moves to the next direct sibling.
	 *
	 * @return `true` if the cursor moved to the next sibling;
	 *         `false` if this is already the last sibling or the cursor is at the root
	 *         (the cursor position remains unchanged).
	 */
	fun forward(): Boolean {
		if (depth == 0) return false
		val parentIndex = path[depth - 1]
		val curr = index

		if (curr == parentIndex - 1) return false

		var candidate = parentIndex - 1
		while (true) {
			val ev = stream.events[candidate]
			val slotCount = if (ev is NodeEvent) 1 + ev.childCount else 1
			val prev = candidate - slotCount
			if (prev == curr) {
				path[depth] = candidate
				return true
			}
			candidate = prev
		}
	}

	/**
	 * Moves to the previous direct sibling.
	 *
	 * @return `true` if the cursor moved to the previous sibling;
	 *         `false` if this is already the first sibling or the cursor is at the root
	 *         (the cursor position remains unchanged).
	 */
	fun backward(): Boolean {
		if (depth == 0) return false
		val parentIndex = path[depth - 1]
		val parentEvent = stream.events[parentIndex] as NodeEvent
		val parentStart = parentIndex - parentEvent.childCount

		val curr = index
		val currEvent = stream.events[curr]
		val slotCount = if (currEvent is NodeEvent) 1 + currEvent.childCount else 1
		val prev = curr - slotCount

		if (prev < parentStart) return false
		path[depth] = prev
		return true
	}

	/**
	 * Resets the cursor back to the root node (depth 0).
	 */
	fun reset() {
		depth = 0
		path[0] = initialIndex
	}

	/**
	 * Runs the given [action] on each direct child of the current node from left to right.
	 *
	 * The cursor is guaranteed to return to its original position when this method finishes,
	 * even if [action] throws an exception or exits early via a non-local return.
	 */
	inline fun forEachChild(action: (SyntaxStreamCursor) -> Unit) {
		if (!down()) return
		val parentDepth = depth - 1
		try {
			do {
				action(this)
			} while (forward())
		} finally {
			while (depth > parentDepth) {
				up()
			}
		}
	}

	private fun ensureCapacity(requiredSize: Int) {
		if (requiredSize > path.size) {
			// Grow by 1.5x
			val newCapacity = path.size + (path.size shr 1)
			path = path.copyOf(maxOf(newCapacity, requiredSize))
		}
	}
}