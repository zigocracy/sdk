package net.landless_city.zigocracy.zig.syntax

import net.landless_city.zigocracy.zig.shared.CodeUnits

sealed interface SyntaxEvent {
	val kind: SyntaxKind
}

@JvmRecord
data class TokenEvent(
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