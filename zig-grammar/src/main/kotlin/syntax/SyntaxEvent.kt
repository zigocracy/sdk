package com.zigocracy.sdk.zig.syntax

import com.zigocracy.sdk.zig.text.CodeUnits

sealed interface SyntaxEvent {
	val kind: SyntaxKind
}

@JvmRecord
data class TokenEvent(
	override val kind: TokenKind,
	val width: CodeUnits,
) : SyntaxEvent {
	/**
	 * True if this token is synthetic and has no physical representation in the source text.
	 */
	val isSynthetic: Boolean get() = width == 0

	public companion object {
		/**
		 * Creates a synthetic, zero-width token of the specified category.
		 */
		fun synthetic(kind: TokenKind): TokenEvent =
			TokenEvent(kind, width = 0)
	}
}

@JvmRecord
data class NodeEvent(
	override val kind: NodeKind,
	val childCount: Int,
) : SyntaxEvent {
	init {
		require(childCount >= 0) { "Node child count cannot be negative: $childCount" }
	}
}