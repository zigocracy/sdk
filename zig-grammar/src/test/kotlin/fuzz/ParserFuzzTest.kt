package net.landless_city.zigocracy.zig.fuzz

import com.code_intelligence.jazzer.junit.FuzzTest
import com.code_intelligence.jazzer.mutation.annotation.WithLength
import net.landless_city.zigocracy.zig.parser.Parser
import net.landless_city.zigocracy.zig.parser.ParserResult
import net.landless_city.zigocracy.zig.scanner.util.ScannerUtils
import net.landless_city.zigocracy.zig.syntax.NodeEvent
import net.landless_city.zigocracy.zig.syntax.TokenEvent
import net.landless_city.zigocracy.zig.syntax.TokenKind
import net.landless_city.zigocracy.zig.text.SourceFile
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Tag
import org.opentest4j.AssertionFailedError

@Tag("fuzz")
class ParserFuzzTest {
	@FuzzTest
	fun `fuzz parser`(text: @WithLength(max = 200) String?) {
		if (text == null) return
		val sourceFile = SourceFile.forTesting(text)
		val result = Parser.parseSyntax(sourceFile)

		try {
			validateSyntaxStreamInvariants(text, result)
		} catch (e: Throwable) {
			throw AssertionFailedError(
				/* message = */ "Fuzzer Invariant Broken: ${e.message}",
				/* expected = */ "No Crash",
				/* actual = */ FuzzFailureReport(text, result).toString(),
				/* cause = */ e
			)
		}
	}

	private fun validateSyntaxStreamInvariants(input: String, result: ParserResult) {
		// Pass 1: Walk through tokens to ensure bounds match the file and verify spacing rules
		validateTokenStream(input, result)

		// Pass 2: Reconstruct the structural nodes with a stack to check tree soundness
		validateTreeStructure(input, result)

		// Pass 3: Ensure diagnostic error ranges align accurately inside the source file boundaries
		validateDiagnosticRanges(input, result)
	}

	private fun validateTokenStream(input: String, result: ParserResult) {
		var currentOffset = 0

		for (event in result.stream.events) {
			if (event is TokenEvent) {
				val tokenWidth = event.width
				val tokenKind = event.kind

				// Rule: Tokens must never cross past the end of the text
				assertTrue(currentOffset + tokenWidth <= input.length) {
					"Token $tokenKind leaked out of bounds! Offset: $currentOffset, Width: $tokenWidth, File Total: ${input.length}"
				}

				val tokenText = input.substring(currentOffset, currentOffset + tokenWidth)

				// Rule: Keywords, identifiers, and symbols must not accidentally swallow spaces or newlines
				if (tokenText.isNotEmpty() && !tokenKind.canEndWithWhitespace()) {
					val lastChar = tokenText.last()
					val isWhitespace = ScannerUtils.isHorizontalWhitespace(lastChar) ||
						ScannerUtils.isVerticalWhitespace(lastChar)

					assertTrue(!isWhitespace) {
						"Structural token $tokenKind with text [${escapeTrivia(tokenText)}] mistakenly included trailing whitespace!"
					}
				}

				currentOffset += tokenWidth
			}
		}

		// Rule: The pipeline must consume the entire input string completely
		assertEquals(input.length, currentOffset) {
			"The parser stopped early and did not consume the full file text! Remainder offset: $currentOffset"
		}
	}

	private fun validateTreeStructure(input: String, result: ParserResult) {
		val widthStack = ArrayDeque<Int>()

		for ((index, event) in result.stream.events.withIndex()) {
			when (event) {
				is TokenEvent -> {
					widthStack.addLast(event.width)
				}

				is NodeEvent -> {
					// Rule: A node cannot declare more children than what is available on the stack
					assertTrue(widthStack.size >= event.childCount) {
						"Tree structure is corrupt! Node needs ${event.childCount} children, but stack only has ${widthStack.size} items."
					}

					var accumulatedSubtreeWidth = 0
					repeat(event.childCount) {
						accumulatedSubtreeWidth += widthStack.removeLast()
					}

					// Rule: Verify that bottom-up stack width matches the tree API response
					val apiComputedWidth = result.stream.computeWidthAt(index)
					assertEquals(apiComputedWidth, accumulatedSubtreeWidth) {
						"Width desynchronization detected at index ${index}! Stack accumulated $accumulatedSubtreeWidth but API reported $apiComputedWidth."
					}

					// Pass the combined width up to the parent node context
					widthStack.addLast(accumulatedSubtreeWidth)
				}
			}
		}

		// Rule: A successful parse must finish with exactly one unified root node representing the entire file
		assertEquals(1, widthStack.size) {
			"The syntax stream did not resolve into a single root node! Leftover items on stack: ${widthStack.size}"
		}

		// Rule: The combined width of the tree must equal the length of the input text
		assertEquals(input.length, widthStack.first()) {
			"The sum of node widths (${widthStack.first()}) does not match the actual file size (${input.length})!"
		}
	}

	private fun validateDiagnosticRanges(input: String, result: ParserResult) {
		for (diag in result.diagnostics) {
			assertAll(
				"Diagnostic boundary validation",
				{ assertTrue(diag.startPosition >= 0) { "Diagnostic error index is negative: ${diag.startPosition}" } },
				{ assertTrue(diag.startPosition + diag.width <= input.length) { "Diagnostic error range extends past the file length! End: ${diag.startPosition + diag.width}, File length: ${input.length}" } }
			)
		}
	}
}

/**
 * Returns true if the token type can naturally end with whitespace.
 * Includes formatting padding, comments, and literal text blocks.
 */
private fun TokenKind.canEndWithWhitespace(): Boolean = when (this) {
	TokenKind.Whitespace,
	TokenKind.Newline,
	TokenKind.Comment,
	TokenKind.DocComment,
	TokenKind.TopLevelDocComment,
	TokenKind.StringLiteral,
	TokenKind.MultilineStringPart,
	TokenKind.CharLiteral -> true

	else -> false
}

/**
 * Replaces raw terminal whitespaces and hidden null indicators with
 * explicit code representations to ensure visible layout verification logs.
 */
private fun escapeTrivia(str: String): String = buildString(capacity = str.length * 2) {
	for (c in str) {
		when (c) {
			'\n' -> append("\\n")
			'\r' -> append("\\r")
			'\t' -> append("\\t")
			'\u0000' -> append("\\u0000")
			else -> append(c)
		}
	}
}

private data class FuzzFailureReport(
	val rawInput: String,
	val result: ParserResult
) {
	override fun toString(): String {
		val offsets = result.stream.events.scan(0) { currentOffset, event ->
			currentOffset + if (event is TokenEvent) event.width else 0
		}

		val eventStreamBlock = result.stream.events.withIndex().joinToString(separator = "\n") { (index, event) ->
			val currentOffset = offsets[index]
			when (event) {
				is TokenEvent -> {
					val endOffset = currentOffset + event.width
					val textChunk = if (endOffset <= rawInput.length) rawInput.substring(currentOffset, endOffset) else "CRITICAL_OUT_OF_BOUNDS"
					"  [%3d] TOKEN : %-25s | Width: %2d | Range: [%2d..%2d] | Text: [%s]".format(
						index, event.kind, event.width, currentOffset, endOffset, escapeTrivia(textChunk)
					)
				}

				is NodeEvent -> {
					"  [%3d] NODE  : %-25s | Declared Child Count: %d".format(
						index, event.kind, event.childCount
					)
				}
			}
		}

		val diagnosticsBlock = result.diagnostics.withIndex()
			.map { (index, diag) ->
				"  [%2d] CODE: %-30s | Range: [%2d..%2d] | Width: %2d".format(
					index, diag.code, diag.startPosition, diag.endPosition, diag.width
				)
			}
			.ifEmpty { listOf("  None (No errors or warnings captured)") }
			.joinToString(separator = "\n")

		return """
			|
			|=== FUZZER CRASH DETECTED ===
			|File Size (Chars) : ${rawInput.length}
			|Escaped File Content : [${escapeTrivia(rawInput)}]
			|
			|--- PARSER EVENT STREAM ---
			|$eventStreamBlock
			|
			|--- DETECTED DIAGNOSTICS ---
			|$diagnosticsBlock
			|===============================
		""".trimMargin()
	}
}