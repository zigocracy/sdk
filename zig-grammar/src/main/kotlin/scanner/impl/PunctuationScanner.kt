package com.zigocracy.sdk.zig.scanner.impl

import com.zigocracy.sdk.zig.scanner.ScanResult
import com.zigocracy.sdk.zig.scanner.Scanner
import com.zigocracy.sdk.zig.syntax.TokenKind
import com.zigocracy.sdk.zig.text.TextReader

/**
 * A fast, fixed-window scanner implementing the Maximal Munch (longest match) principle
 * for punctuation marks, multi-character operators, and assignment sequences.
 */
internal object PunctuationScanner : Scanner {
	private val symbols: Map<String, TokenKind> = mapOf(
		// --- 4 characters ---
		"<<|=" to TokenKind.ShlSaturateAssign,
		"*%=" to TokenKind.AsteriskWrapAssign,
		"*|=" to TokenKind.AsteriskSaturateAssign,
		"+%=" to TokenKind.PlusWrapAssign,
		"+|=" to TokenKind.PlusSaturateAssign,
		"-%=" to TokenKind.MinusWrapAssign,
		"-|=" to TokenKind.MinusSaturateAssign,

		// --- 3 characters ---
		"..." to TokenKind.Ellipsis,
		"<<=" to TokenKind.ShlAssign,
		"<<|" to TokenKind.ShlSaturate,
		">>=" to TokenKind.ShrAssign,

		// --- 2 characters ---
		"!=" to TokenKind.NotEquals,
		"%=" to TokenKind.PercentAssign,
		"&=" to TokenKind.BitwiseAndAssign,
		"*%" to TokenKind.AsteriskWrap,
		"**" to TokenKind.Repeat,
		"*=" to TokenKind.AsteriskAssign,
		"*|" to TokenKind.AsteriskSaturate,
		"+%" to TokenKind.PlusWrap,
		"++" to TokenKind.Concat,
		"+=" to TokenKind.PlusAssign,
		"+|" to TokenKind.PlusSaturate,
		"-%" to TokenKind.MinusWrap,
		"-=" to TokenKind.MinusAssign,
		"->" to TokenKind.SkinnyArrow,
		"-|" to TokenKind.MinusSaturate,
		".*" to TokenKind.PtrDereference,
		".." to TokenKind.Range,
		".?" to TokenKind.OptionalUnwrap,
		"/=" to TokenKind.SlashAssign,
		"<<" to TokenKind.Shl,
		"<=" to TokenKind.LessEqual,
		"==" to TokenKind.Equals,
		"=>" to TokenKind.FatArrow,
		">=" to TokenKind.GreaterEqual,
		">>" to TokenKind.Shr,
		"^=" to TokenKind.BitwiseXorAssign,
		"|=" to TokenKind.BitwiseOrAssign,
		"||" to TokenKind.LogicalOr,

		// --- 1 character ---
		"!" to TokenKind.ExclamationMark,
		"%" to TokenKind.Percent,
		"&" to TokenKind.BitwiseAnd,
		"(" to TokenKind.LeftParen,
		")" to TokenKind.RightParen,
		"*" to TokenKind.Asterisk,
		"+" to TokenKind.Plus,
		"," to TokenKind.Comma,
		"-" to TokenKind.Minus,
		"." to TokenKind.Dot,
		"/" to TokenKind.Slash,
		":" to TokenKind.Colon,
		";" to TokenKind.Semicolon,
		"<" to TokenKind.LessThan,
		"=" to TokenKind.Assign,
		">" to TokenKind.GreaterThan,
		"?" to TokenKind.QuestionMark,
		"[" to TokenKind.LeftBracket,
		"]" to TokenKind.RightBracket,
		"^" to TokenKind.BitwiseXor,
		"{" to TokenKind.LeftBrace,
		"|" to TokenKind.BitwiseOr,
		"}" to TokenKind.RightBrace,
		"~" to TokenKind.Tilde
	)

	private const val MAX_SYMBOL_WIDTH = 4

	override fun scan(reader: TextReader): ScanResult {
		// Slices a maximum possible lookup window to avoid repeated nested reader invocations.
		val maxCandidate = reader.peekString(MAX_SYMBOL_WIDTH)
		if (maxCandidate.isEmpty()) return ScanResult.NoMatch

		// Examples of downward slicing logic (Maximal Munch):
		// - "<<|=" -> Iteration 1 checks "<<|=" -> Immediately matches ShlSaturateAssign with width 4.
		// - "<<| " -> Iteration 1 fails ("<<| "), Iteration 2 checks "<<|" -> Matches ShlSaturate with width 3.
		// - "+foo" -> Iterations 1-3 fail, Iteration 4 checks "+" -> Matches Plus with width 1.
		for (width in maxCandidate.length downTo 1) {
			val candidate = maxCandidate.substring(0, width)
			val tokenKind = symbols[candidate]

			if (tokenKind != null) {
				return ScanResult.Matched(
					tokenKind,
					width,
					diagnostics = emptyList()
				)
			}
		}

		return ScanResult.NoMatch
	}
}