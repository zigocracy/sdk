package com.zigocracy.sdk.lsp.analysis

import com.zigocracy.sdk.lsp.server.ZigocracyLanguageServer
import com.zigocracy.sdk.zig.syntax.TokenEvent
import com.zigocracy.sdk.zig.syntax.VisualGroup
import com.zigocracy.sdk.zig.syntax.classifyToVisualGroup

internal class LspTokenCollector(private val snapshot: DocumentSnapshot) {
	private val tokensData = mutableListOf<Int>()

	fun collectAndEncode(): List<Int> {
		val stream = snapshot.stream
		val lineMap = snapshot.source.lineMap

		var absOffset = 0
		var prevLine = 0
		var prevChar = 0

		for (event in stream.events) {
			if (event is TokenEvent) {
				val group = event.kind.classifyToVisualGroup()

				if (group != VisualGroup.Whitespace && group != VisualGroup.Newline && event.width > 0) {
					val coords = lineMap.getCoordinates(absOffset)

					val currentLine = coords.line - 1
					val currentChar = coords.column - 1

					val typeIndex = ZigocracyLanguageServer.VISUAL_GROUP_TYPE_INDICES.getValue(group)

					if (typeIndex >= 0) {
						val deltaLine = currentLine - prevLine
						val deltaStartChar = if (deltaLine == 0) currentChar - prevChar else currentChar

						val modifierMask = ZigocracyLanguageServer.VISUAL_GROUP_MODIFIERS.getOrDefault(group, 0)

						tokensData.add(deltaLine)
						tokensData.add(deltaStartChar)
						tokensData.add(event.width)
						tokensData.add(typeIndex)
						tokensData.add(modifierMask)

						prevLine = currentLine
						prevChar = currentChar
					}
				}

				absOffset += event.width
			}
		}

		return tokensData
	}
}