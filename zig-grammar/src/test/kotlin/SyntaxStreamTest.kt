package com.zigocracy.sdk.zig

import com.zigocracy.sdk.zig.syntax.NodeKind
import com.zigocracy.sdk.zig.syntax.SyntaxStreamBuilder
import com.zigocracy.sdk.zig.syntax.TokenKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SyntaxStreamTest {
	@Nested
	inner class WidthComputation {

		@Test
		fun `computes exact width for structural nodes`() {
			val builder = SyntaxStreamBuilder()
			val rootMark = builder.recordStart()
			val literalMark = builder.recordStart()

			builder.addToken(TokenKind.Identifier, width = 4)
			builder.emitNode(literalMark, NodeKind.LiteralExpression)
			builder.emitNode(rootMark, NodeKind.File)

			val stream = builder.build()

			assertEquals(4, stream.computeWidthAt(1)) { "Node width must match wrapped tokens." }
			assertEquals(4, stream.computeWidthAt(2)) { "Root node width must cover all contents." }
		}

		@Test
		fun `includes whitespace widths in lossless nodes`() {
			val builder = SyntaxStreamBuilder()
			val rootMark = builder.recordStart()
			val literalMark = builder.recordStart()

			builder.addToken(TokenKind.Identifier, width = 4)
			builder.addToken(TokenKind.Whitespace, width = 2)
			builder.emitNode(literalMark, NodeKind.LiteralExpression)
			builder.emitNode(rootMark, NodeKind.File)

			val stream = builder.build()

			assertEquals(6, stream.computeWidthAt(2)) { "Lossless nodes must accumulate inner whitespace width." }
		}

		@Test
		fun `computes exact width for structural node containing other structural nodes`() {
			val builder = SyntaxStreamBuilder()

			val outerNodeMark = builder.recordStart()
			val innerNodeMark = builder.recordStart()

			builder.addToken(TokenKind.Identifier, width = 4)
			builder.emitNode(innerNodeMark, NodeKind.LiteralExpression)

			val anotherInnerMark = builder.recordStart()
			builder.emitNode(anotherInnerMark, NodeKind.LiteralExpression)

			builder.emitNode(outerNodeMark, NodeKind.File)

			val stream = builder.build()

			assertEquals(4, stream.computeWidthAt(1))
			assertEquals(0, stream.computeWidthAt(2))
			assertEquals(4, stream.computeWidthAt(3))
		}


	}

	@Nested
	inner class StructuralInvariants {

		@Test
		fun `nested node markers yield zero width to avoid double counting`() {
			val builder = SyntaxStreamBuilder()
			val rootMark = builder.recordStart()
			val literalMark = builder.recordStart()

			builder.addToken(TokenKind.Identifier, width = 4)
			builder.emitNode(literalMark, NodeKind.LiteralExpression)
			builder.emitNode(rootMark, NodeKind.File)

			val stream = builder.build()

			assertEquals(4, stream.computeWidthAt(2)) { "Evaluation pipeline must ignore nested nodes to prevent duplicating width." }
		}

		@Test
		fun `handles empty nodes with zero children safely`() {
			val builder = SyntaxStreamBuilder()
			val rootMark = builder.recordStart()

			builder.emitNode(rootMark, NodeKind.File)

			val stream = builder.build()

			assertEquals(0, stream.computeWidthAt(0)) { "Nodes with no children must evaluate to 0 width." }
		}
	}

	@Nested
	inner class ErrorHandling {

		@Test
		fun `fails lazily when evaluating a mark from the past`() {
			val builder = SyntaxStreamBuilder()
			val rootMark = builder.recordStart()

			builder.addToken(TokenKind.Identifier, width = 4)

			val corruptedMark = SyntaxStreamBuilder.StartMark(startIndex = -1)
			builder.emitNode(corruptedMark, NodeKind.LiteralExpression)

			builder.emitNode(rootMark, NodeKind.File)

			val stream = builder.build()

			assertThrows(IllegalArgumentException::class.java) {
				stream.computeWidthAt(1)
			}
		}

		@Test
		fun `fails immediately when completing a mark from the future`() {
			val builder = SyntaxStreamBuilder()

			builder.addToken(TokenKind.Identifier, width = 4)

			val futureMark = SyntaxStreamBuilder.StartMark(startIndex = 50)

			assertThrows(IllegalArgumentException::class.java) {
				builder.emitNode(futureMark, NodeKind.LiteralExpression)
			}
		}
	}
}
