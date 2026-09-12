package com.zigocracy.sdk.engine.vfs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class LineMapTest {

	@Nested
	inner class GetCoordinates {
		@Test
		fun `computes coordinates for single line text`() {
			val text = "const x = 42;"
			val lineMap = LineMap.buildFor(text)

			assertEquals(1, lineMap.getLineCount())
			assertEquals(LineMap.Coordinates(line = 1, column = 1), lineMap.getCoordinates(0)) // 'c'
			assertEquals(LineMap.Coordinates(line = 1, column = 7), lineMap.getCoordinates(6)) // 'x'
			assertEquals(LineMap.Coordinates(line = 1, column = 14), lineMap.getCoordinates(13)) // EOF
		}

		@Test
		fun `computes coordinates across newlines`() {
			val text = "first\nsecond\nthird"
			val lineMap = LineMap.buildFor(text)

			assertEquals(3, lineMap.getLineCount())
			assertEquals(LineMap.Coordinates(line = 1, column = 1), lineMap.getCoordinates(0)) // 'f'
			assertEquals(LineMap.Coordinates(line = 1, column = 6), lineMap.getCoordinates(5)) // '\n'
			assertEquals(LineMap.Coordinates(line = 2, column = 1), lineMap.getCoordinates(6)) // 's'
			assertEquals(LineMap.Coordinates(line = 3, column = 1), lineMap.getCoordinates(13)) // 't'
		}

		@Test
		fun `computes coordinates across different newlines`() {
			val text = "win\r\nmac\rend"
			val lineMap = LineMap.buildFor(text)

			assertEquals(3, lineMap.getLineCount())
			assertEquals(LineMap.Coordinates(line = 1, column = 1), lineMap.getCoordinates(0)) // 'w'
			assertEquals(LineMap.Coordinates(line = 2, column = 1), lineMap.getCoordinates(5)) // 'm' (after \r\n)
			assertEquals(LineMap.Coordinates(line = 3, column = 1), lineMap.getCoordinates(9)) // 'e' (after \r)
		}

		@Test
		fun `clamps out of bounds offsets`() {
			val text = "hello"
			val lineMap = LineMap.buildFor(text)

			assertEquals(LineMap.Coordinates(line = 1, column = 1), lineMap.getCoordinates(-5)) // underflow
			assertEquals(LineMap.Coordinates(line = 1, column = 6), lineMap.getCoordinates(100)) // overflow
		}
	}

	@Nested
	inner class GetLineRange {
		@Test
		fun `retrieves line offset ranges correctly`() {
			val text = "abc\ndef\r\ng"
			val lineMap = LineMap.buildFor(text)

			assertEquals(0..<4, lineMap.getLineRange(0)) // "abc\n"
			assertEquals(4..<9, lineMap.getLineRange(1)) // "def\r\n"
			assertEquals(9..<10, lineMap.getLineRange(2)) // "g"
		}
	}
}