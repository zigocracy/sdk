package net.landless_city.zigocracy.zig

sealed interface SyntaxKind

enum class TokenKind : SyntaxKind {
	KeywordTrue,
	Whitespace
}

enum class NodeKind : SyntaxKind {
	BooleanLiteral,
	File
}