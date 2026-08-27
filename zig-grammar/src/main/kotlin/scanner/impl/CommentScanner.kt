package com.zigocracy.sdk.zig.scanner.impl

import com.zigocracy.sdk.zig.scanner.ScanDiagnostics
import com.zigocracy.sdk.zig.scanner.ScanResult
import com.zigocracy.sdk.zig.scanner.Scanner
import com.zigocracy.sdk.zig.scanner.util.isZigVerticalWhitespace
import com.zigocracy.sdk.zig.shared.DiagnosticCode
import com.zigocracy.sdk.zig.syntax.TokenKind
import com.zigocracy.sdk.zig.text.TextReader

internal object CommentScanner : Scanner {
	override fun scan(reader: TextReader): ScanResult {
		val prefix = reader.peekString(width = 4)
		if (!prefix.startsWith("//")) return ScanResult.NoMatch

		val diagnostics = mutableListOf<ScanDiagnostics>()

		// Examples of comment token classification routing:
		// - "//! global doc"  -> Matches top-level module documentation tag.
		// - "/// function doc" -> Matches standard declaration documentation tag.
		// - "// normal comment" -> Matches regular implementation comment.
		// - "//// ambiguous"    -> Flags syntax error but falls back cleanly to a regular Comment token.
		val kind = when {
			prefix.startsWith("//!") -> TokenKind.TopLevelDocComment
			prefix.startsWith("///") -> {
				if (prefix.startsWith("////")) {
					diagnostics.add(ScanDiagnostics(DiagnosticCode.CommentError.AmbiguousCommentStyle, 0, 4))
					TokenKind.Comment
				} else {
					TokenKind.DocComment
				}
			}

			else -> TokenKind.Comment
		}

		var width = 2

		// Examples of greedy line consumption:
		// - "// text\n"   -> Stops exactly at the line break, leaving '\n' to be processed as a structural Newline token.
		// - "// text"     -> Gracefully stops at EOF boundary without index out of bounds crashes.
		// - "// text\r\n" -> Terminating boundary catches '\r' immediately, preserving Windows line ending layouts.
		while (true) {
			val c = reader.peekChar(width)

			if (c == null || c.isZigVerticalWhitespace()) {
				break
			}

			width++
		}

		return ScanResult.Matched(
			kind,
			width,
			diagnostics
		)
	}
}