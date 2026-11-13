package com.zigocracy.sdk.zig

import com.zigocracy.sdk.zig.syntax.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SyntaxStreamCursorTest {

	@Nested
	inner class HierarchicalInspection {

		@Test
		fun `exploring declaration descends to direct children and ascends back to file scope`() {
			val builder = SyntaxStreamBuilder()
			val fileMark = builder.recordStart()
			val varMark = builder.recordStart()
			builder.addToken(TokenKind.Identifier, width = 3)
			builder.emitNode(varMark, NodeKind.VarDeclaration)
			builder.emitNode(fileMark, NodeKind.File)

			val cursor = builder.build().cursor()

			// Start at the file level
			assertEquals(NodeKind.File, cursor.event.kind)
			assertEquals(0, cursor.depth)

			// Step into the variable declaration
			assertTrue(cursor.down())
			assertEquals(NodeKind.VarDeclaration, cursor.event.kind)
			assertEquals(1, cursor.depth)

			// Step back out to the enclosing file
			assertTrue(cursor.up())
			assertEquals(NodeKind.File, cursor.event.kind)
			assertEquals(0, cursor.depth)
		}

		@Test
		fun `drilling through nested expression chain preserves exact depth at each level`() {
			val builder = SyntaxStreamBuilder()
			val fileMark = builder.recordStart()
			val varMark = builder.recordStart()
			val exprMark = builder.recordStart()

			builder.addToken(TokenKind.Identifier, width = 2)
			builder.emitNode(exprMark, NodeKind.IdentifierExpression)
			builder.emitNode(varMark, NodeKind.VarDeclaration)
			builder.emitNode(fileMark, NodeKind.File)

			val cursor = builder.build().cursor()

			// Descend File -> VarDeclaration -> IdentifierExpression -> Identifier token
			assertTrue(cursor.down())
			assertEquals(NodeKind.VarDeclaration, cursor.event.kind)
			assertEquals(1, cursor.depth)

			assertTrue(cursor.down())
			assertEquals(NodeKind.IdentifierExpression, cursor.event.kind)
			assertEquals(2, cursor.depth)

			assertTrue(cursor.down())
			assertEquals(TokenKind.Identifier, cursor.event.kind)
			assertEquals(3, cursor.depth)

			// Ascend back through every enclosing scope
			assertTrue(cursor.up())
			assertEquals(NodeKind.IdentifierExpression, cursor.event.kind)
			assertEquals(2, cursor.depth)

			assertTrue(cursor.up())
			assertEquals(NodeKind.VarDeclaration, cursor.event.kind)
			assertEquals(1, cursor.depth)

			assertTrue(cursor.up())
			assertEquals(NodeKind.File, cursor.event.kind)
			assertEquals(0, cursor.depth)
		}
	}

	@Nested
	inner class SequentialScanning {

		@Test
		fun `advancing between sibling statements leaps across nested expressions`() {
			val builder = SyntaxStreamBuilder()
			val fileMark = builder.recordStart()

			// Sibling 1: VarDeclaration containing a token and a nested LiteralExpression
			val varMark = builder.recordStart()
			builder.addToken(TokenKind.Identifier, width = 3)
			val exprMark = builder.recordStart()
			builder.addToken(TokenKind.IntegerLiteral, width = 2)
			builder.emitNode(exprMark, NodeKind.LiteralExpression)
			builder.emitNode(varMark, NodeKind.VarDeclaration)

			// Sibling 2: Semicolon token separating statements
			builder.addToken(TokenKind.Semicolon, width = 1)

			// Sibling 3: Another statement
			val stmtMark = builder.recordStart()
			builder.addToken(TokenKind.Identifier, width = 4)
			builder.emitNode(stmtMark, NodeKind.PlaceholderStatement)

			builder.emitNode(fileMark, NodeKind.File)

			val cursor = builder.build().cursor()
			assertTrue(cursor.down())
			assertEquals(NodeKind.VarDeclaration, cursor.event.kind)

			// Moving forward leaps over nested tokens and literal expressions directly to semicolon
			assertTrue(cursor.forward())
			assertEquals(TokenKind.Semicolon, cursor.event.kind)
			assertEquals(1, cursor.depth)

			// Moving forward again reaches the next statement
			assertTrue(cursor.forward())
			assertEquals(NodeKind.PlaceholderStatement, cursor.event.kind)
			assertEquals(1, cursor.depth)
		}

		@Test
		fun `reversing between sibling statements returns to preceding statement header`() {
			val builder = SyntaxStreamBuilder()
			val fileMark = builder.recordStart()

			val varMark = builder.recordStart()
			builder.addToken(TokenKind.Identifier, width = 3)
			builder.emitNode(varMark, NodeKind.VarDeclaration)

			builder.addToken(TokenKind.Semicolon, width = 1)
			builder.emitNode(fileMark, NodeKind.File)

			val cursor = builder.build().cursor()
			assertTrue(cursor.down())
			assertTrue(cursor.forward())
			assertEquals(TokenKind.Semicolon, cursor.event.kind)

			// Stepping backward returns to VarDeclaration header, not its inner tokens
			assertTrue(cursor.backward())
			assertEquals(NodeKind.VarDeclaration, cursor.event.kind)
			assertEquals(1, cursor.depth)
		}

		@Test
		fun `scanning past sibling boundaries halts and preserves current position`() {
			val builder = SyntaxStreamBuilder()
			val fileMark = builder.recordStart()

			val varMark = builder.recordStart()
			builder.addToken(TokenKind.Identifier, width = 3)
			builder.emitNode(varMark, NodeKind.VarDeclaration)

			builder.addToken(TokenKind.Semicolon, width = 1)
			builder.emitNode(fileMark, NodeKind.File)

			val cursor = builder.build().cursor()
			assertTrue(cursor.down())
			assertEquals(NodeKind.VarDeclaration, cursor.event.kind)

			// First sibling has no previous sibling
			assertFalse(cursor.backward())
			assertEquals(NodeKind.VarDeclaration, cursor.event.kind)

			// Advance to last sibling
			assertTrue(cursor.forward())
			assertEquals(TokenKind.Semicolon, cursor.event.kind)

			// Last sibling has no next sibling
			assertFalse(cursor.forward())
			assertEquals(TokenKind.Semicolon, cursor.event.kind)
		}
	}

	@Nested
	inner class ChildIteration {

		@Test
		fun `iterating container visits every top-level member in reading order`() {
			val builder = SyntaxStreamBuilder()
			val fileMark = builder.recordStart()

			val varMark = builder.recordStart()
			builder.addToken(TokenKind.Identifier, width = 4)
			builder.emitNode(varMark, NodeKind.VarDeclaration)

			builder.addToken(TokenKind.Semicolon, width = 1)
			builder.emitNode(fileMark, NodeKind.File)

			val cursor = builder.build().cursor()
			val visited = mutableListOf<String>()

			cursor.forEachChild { child ->
				visited.add("${child.depth}:${child.event.kind}")
			}

			assertEquals(listOf("1:VarDeclaration", "1:Semicolon"), visited)
			assertEquals(NodeKind.File, cursor.event.kind)
			assertEquals(0, cursor.depth)
		}

		@Test
		fun `aborting child iteration with early return unwinds cursor back to parent`() {
			val builder = SyntaxStreamBuilder()
			val fileMark = builder.recordStart()

			val varMark = builder.recordStart()
			builder.addToken(TokenKind.Identifier, width = 4)
			builder.emitNode(varMark, NodeKind.VarDeclaration)

			builder.addToken(TokenKind.Semicolon, width = 1)
			builder.emitNode(fileMark, NodeKind.File)

			val cursor = builder.build().cursor()

			fun findFirstDeclaration(): SyntaxKind? {
				cursor.forEachChild { child ->
					if (child.event.kind == NodeKind.VarDeclaration) {
						return child.event.kind // non-local return
					}
				}
				return null
			}

			val result = findFirstDeclaration()
			assertEquals(NodeKind.VarDeclaration, result)
			assertEquals(NodeKind.File, cursor.event.kind, "Must unwind to parent after non-local return")
			assertEquals(0, cursor.depth)
		}

		@Test
		fun `iterating over empty node or leaf token performs no work`() {
			val builder = SyntaxStreamBuilder()
			val fileMark = builder.recordStart()
			builder.addToken(TokenKind.Identifier, width = 2)
			builder.emitNode(fileMark, NodeKind.File)

			val cursor = builder.build().cursor()
			assertTrue(cursor.down())
			assertEquals(TokenKind.Identifier, cursor.event.kind)

			var invocationCount = 0
			cursor.forEachChild { invocationCount++ }

			assertEquals(0, invocationCount)
			assertEquals(TokenKind.Identifier, cursor.event.kind)
			assertEquals(1, cursor.depth)
		}
	}

	@Nested
	inner class BoundaryConditions {

		@Test
		fun `empty syntax node cannot be entered and maintains position`() {
			val builder = SyntaxStreamBuilder()
			val fileMark = builder.recordStart()
			val emptyMark = builder.recordStart()
			builder.emitNode(emptyMark, NodeKind.PlaceholderStatement) // slotCount = 0
			builder.emitNode(fileMark, NodeKind.File)

			val cursor = builder.build().cursor()
			assertTrue(cursor.down())
			assertEquals(NodeKind.PlaceholderStatement, cursor.event.kind)

			assertFalse(cursor.down(), "Empty node with zero slotCount cannot be descended into")
			assertEquals(NodeKind.PlaceholderStatement, cursor.event.kind)
			assertEquals(1, cursor.depth)
		}

		@Test
		fun `terminal token rejects descent and maintains position`() {
			val builder = SyntaxStreamBuilder()
			val fileMark = builder.recordStart()
			builder.addToken(TokenKind.Semicolon, width = 1)
			builder.emitNode(fileMark, NodeKind.File)

			val cursor = builder.build().cursor()
			assertTrue(cursor.down())
			assertEquals(TokenKind.Semicolon, cursor.event.kind)

			assertFalse(cursor.down(), "Terminal token cannot be descended into")
			assertEquals(TokenKind.Semicolon, cursor.event.kind)
			assertEquals(1, cursor.depth)
		}

		@Test
		fun `root node refuses horizontal motion and upward escape`() {
			val builder = SyntaxStreamBuilder()
			val fileMark = builder.recordStart()
			builder.addToken(TokenKind.Identifier, width = 1)
			builder.emitNode(fileMark, NodeKind.File)

			val cursor = builder.build().cursor()

			assertFalse(cursor.up(), "Root node has no parent")
			assertFalse(cursor.forward(), "Root node has no next sibling")
			assertFalse(cursor.backward(), "Root node has no previous sibling")
			assertEquals(NodeKind.File, cursor.event.kind)
			assertEquals(0, cursor.depth)
		}

		@Test
		fun `reset restores cursor to file root after deep traversal`() {
			val builder = SyntaxStreamBuilder()
			val fileMark = builder.recordStart()
			val varMark = builder.recordStart()
			builder.addToken(TokenKind.Identifier, width = 4)
			builder.emitNode(varMark, NodeKind.VarDeclaration)
			builder.emitNode(fileMark, NodeKind.File)

			val cursor = builder.build().cursor()

			cursor.down()
			cursor.down()
			assertEquals(2, cursor.depth)

			cursor.reset()
			assertEquals(NodeKind.File, cursor.event.kind)
			assertEquals(2, cursor.index)
			assertEquals(0, cursor.depth)
		}

		@Test
		fun `rejects initialization on coordinates outside stream boundaries`() {
			val builder = SyntaxStreamBuilder()
			val fileMark = builder.recordStart()
			builder.addToken(TokenKind.Identifier, width = 1)
			builder.emitNode(fileMark, NodeKind.File)

			val stream = builder.build()

			assertThrows(IllegalArgumentException::class.java) {
				SyntaxStreamCursor(stream, initialIndex = -1)
			}
			assertThrows(IllegalArgumentException::class.java) {
				SyntaxStreamCursor(stream, initialIndex = stream.events.size)
			}
		}
	}
}