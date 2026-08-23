package net.landless_city.zigocracy.zig.text

class LineMap private constructor(
	private val lineStarts: IntArray,
	private val textLength: Int
) {
	/**
	 * Line numbers and column numbers start from 1.
	 */
	@JvmRecord
	data class Coordinates(val line: Int, val column: Int)

	/**
	 * Calculate line number and column number by absolute offset.
	 *
	 * Time Complexity: Θ(log L), where L is the total number of lines.
	 *
	 * Memory Complexity: Θ(1)
	 */
	fun getCoordinates(absoluteOffset: Int): Coordinates {
		val clampedOffset = absoluteOffset.coerceIn(0, textLength)

		val binarySearchResult = lineStarts.binarySearch(clampedOffset)

		val lineIndex = if (binarySearchResult >= 0)
			binarySearchResult
		else
			(-binarySearchResult - 1) - 1

		val finalLineIndex = lineIndex.coerceAtLeast(0)

		val lineStartOffset = lineStarts[finalLineIndex]
		val column = clampedOffset - lineStartOffset

		return Coordinates(line = finalLineIndex + 1, column = column + 1)
	}

	fun getLineCount(): Int = lineStarts.size

	/**
	 * Returns the absolute start (inclusive) and end (exclusive) offset range for a 0-based line index.
	 *
	 * Time Complexity: Θ(1)
	 * Memory Complexity: Θ(1)
	 */
	fun getLineRange(lineIndex: Int): IntRange {
		val index = lineIndex.coerceIn(0, lineStarts.size - 1)
		val start = lineStarts[index]
		val end = if (index + 1 < lineStarts.size) lineStarts[index + 1] else textLength
		return start..<end
	}

	companion object {
		/**
		 * Time Complexity: Θ(N), where N is the length of the source document string.
		 *
		 * Memory Complexity: Θ(L), where L is the total number of lines.
		 */
		fun buildFor(text: CharSequence): LineMap {
			val starts = scanLineStarts(text)

			return LineMap(starts, text.length)
		}

		private enum class NewlineKind(val step: Int) {
			None(step = 1),
			Unix(step = 1),
			Mac(step = 1),
			Windows(step = 2)
		}

		private fun scanLineStarts(text: CharSequence): IntArray = buildList {
			add(0)

			var index = 0
			val length = text.length
			while (index < length) {
				val char = text[index]
				val kind = classifyCharacter(char, text, index)

				index += kind.step

				if (kind != NewlineKind.None) {
					add(index)
				}
			}
		}.toIntArray()

		private fun classifyCharacter(char: Char, text: CharSequence, index: Int): NewlineKind =
			when (char) {
				'\n' -> NewlineKind.Unix
				'\r' -> if (index + 1 < text.length && text[index + 1] == '\n') NewlineKind.Windows else NewlineKind.Mac
				else -> NewlineKind.None
			}
	}
}
