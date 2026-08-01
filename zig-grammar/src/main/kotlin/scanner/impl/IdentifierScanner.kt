package net.landless_city.zigocracy.zig.scanner.impl

import net.landless_city.zigocracy.zig.scanner.ScanResult
import net.landless_city.zigocracy.zig.scanner.TextReader
import net.landless_city.zigocracy.zig.scanner.TokenDiagnostic
import net.landless_city.zigocracy.zig.scanner.TokenScanner
import net.landless_city.zigocracy.zig.scanner.util.isZigUserIdentifierPart
import net.landless_city.zigocracy.zig.scanner.util.isZigUserIdentifierStart
import net.landless_city.zigocracy.zig.syntax.TokenKind

object IdentifierScanner : TokenScanner {
	private val reservedTokens: Map<String, TokenKind> = mapOf(
		// --- 11 characters ---
		"threadlocal" to TokenKind.ThreadlocalKeyword,
		"unreachable" to TokenKind.UnreachableKeyword,
		"linksection" to TokenKind.LinksectionKeyword,

		// --- 9 characters ---
		"addrspace" to TokenKind.AddrspaceKeyword,
		"allowzero" to TokenKind.AllowzeroKeyword,
		"nosuspend" to TokenKind.NosuspendKeyword,
		"errdefer" to TokenKind.ErrdeferKeyword,
		"comptime" to TokenKind.ComptimeKeyword,
		"callconv" to TokenKind.CallconvKeyword,
		"continue" to TokenKind.ContinueKeyword,
		"volatile" to TokenKind.VolatileKeyword,

		// --- 8 characters ---
		"anyframe" to TokenKind.AnyframeKeyword,
		"noinline" to TokenKind.NoinlineKeyword,

		// --- 7 characters ---
		"anytype" to TokenKind.AnytypeKeyword,
		"noalias" to TokenKind.NoaliasKeyword,
		"suspend" to TokenKind.SuspendKeyword,

		// --- 6 characters ---
		"packed" to TokenKind.PackedKeyword,
		"struct" to TokenKind.StructKeyword,
		"resume" to TokenKind.ResumeKeyword,
		"return" to TokenKind.ReturnKeyword,
		"export" to TokenKind.ExportKeyword,
		"extern" to TokenKind.ExternKeyword,
		"inline" to TokenKind.InlineKeyword,
		"opaque" to TokenKind.OpaqueKeyword,
		"orelse" to TokenKind.OrelseKeyword,
		"switch" to TokenKind.SwitchKeyword,

		// --- 5 characters ---
		"align" to TokenKind.AlignKeyword,
		"break" to TokenKind.BreakKeyword,
		"catch" to TokenKind.CatchKeyword,
		"defer" to TokenKind.DeferKeyword,
		"error" to TokenKind.ErrorKeyword,
		"while" to TokenKind.WhileKeyword,
		"union" to TokenKind.UnionKeyword,
		"const" to TokenKind.ConstKeyword,

		// --- 4 characters ---
		"enum" to TokenKind.EnumKeyword,
		"test" to TokenKind.TestKeyword,
		"else" to TokenKind.ElseKeyword,

		// --- 3 characters ---
		"and" to TokenKind.AndKeyword,
		"asm" to TokenKind.AsmKeyword,
		"for" to TokenKind.ForKeyword,
		"pub" to TokenKind.PubKeyword,
		"try" to TokenKind.TryKeyword,
		"var" to TokenKind.VarKeyword,

		// --- 2 characters ---
		"fn" to TokenKind.FnKeyword,
		"if" to TokenKind.IfKeyword,
		"or" to TokenKind.OrKeyword,

		// --- 1 character ---
		"_" to TokenKind.BlankIdentifier
	)

	override fun scan(reader: TextReader): ScanResult {
		val firstChar = reader.peekChar(0) ?: return ScanResult.NoMatch

		return if (firstChar == '@' && reader.peekChar(1) == '"') {
			scanEscapedIdentifier(reader)
		} else {
			scanPlainIdentifierOrKeyword(reader, firstChar)
		}
	}

	private fun scanEscapedIdentifier(reader: TextReader): ScanResult {
		val diagnostics = mutableListOf<TokenDiagnostic>()

		// Examples of offset base shifts:
		// - `@"while"` -> `base = 1` starts directly at the opening quote character.
		//                  `StringScanner` consumes the rest, ensuring `totalWidth` is exactly 8.
		val stringWidth = StringScanner.scanStringBody(reader, base = 1, diagnostics)
		val totalWidth = 1 + stringWidth

		return ScanResult.Matched(
			TokenKind.Identifier,
			totalWidth,
			diagnostics
		)
	}

	private fun scanPlainIdentifierOrKeyword(reader: TextReader, firstChar: Char): ScanResult {
		// Examples of surrogate pair extraction:
		// - '𝚪' (Gamma, U+1D6AA) -> `isHighSurrogate()` checks true. We pull the trailing character
		//                           at index 1 and decode them into a single 32-bit CodePoint.
		val firstCodePoint = if (firstChar.isHighSurrogate()) {
			val low = reader.peekChar(1) ?: return ScanResult.NoMatch
			Character.toCodePoint(firstChar, low)
		} else {
			firstChar.code
		}

		if (!firstCodePoint.isZigUserIdentifierStart()) return ScanResult.NoMatch

		// Examples of initial layout stride configuration:
		// - "foo" -> Standard single-byte character; sets width pointer to 1.
		// - "𝚪Var" -> Multi-byte surrogate pair block; sets width pointer to 2 to bypass both UTF-16 units.
		var width = if (firstChar.isHighSurrogate()) 2 else 1

		while (true) {
			val c = reader.peekChar(width) ?: break

			// Standardizes 16-bit text units to 32-bit logical code points on the fly
			val codePoint = if (c.isHighSurrogate()) {
				val low = reader.peekChar(width + 1) ?: break
				Character.toCodePoint(c, low)
			} else {
				c.code
			}

			if (codePoint.isZigUserIdentifierPart()) {
				width += if (c.isHighSurrogate()) 2 else 1
			} else {
				break
			}
		}

		// Examples of lexical resolution constraints:
		// - "fn"   -> Slices exactly 2 characters, matches against map, and yields `TokenKind.FnKeyword`.
		// - "fn_x" -> Accumulates text up to the end boundary, fails map check, and defaults to `TokenKind.Identifier`.
		val text = reader.peekString(width)
		val tokenKind = reservedTokens[text] ?: TokenKind.Identifier

		return ScanResult.Matched(
			tokenKind,
			width,
			diagnostics = emptyList()
		)
	}
}