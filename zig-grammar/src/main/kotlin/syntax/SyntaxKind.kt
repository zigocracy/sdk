package com.zigocracy.sdk.zig.syntax

sealed interface SyntaxKind

enum class TokenKind : SyntaxKind {
	// region Punctuation
	LeftParen,      // (
	RightParen,     // )
	LeftBrace,      // {
	RightBrace,     // }
	LeftBracket,    // [
	RightBracket,   // ]
	Comma,       // ,
	Semicolon,   // ;
	Colon,       // :
	Dot,         // .
	Range,      // ..
	Ellipsis,       // ...
	// endregion

	// region Operator
	Assign,            // =
	FatArrow,          // =>
	SkinnyArrow,       // ->
	QuestionMark,      // ?
	ExclamationMark,   // !
	Tilde,             // ~

	PtrDereference,    // .*
	OptionalUnwrap,    // .?

	Plus,              // +
	Minus,             // -
	Asterisk,          // *
	Slash,             // /
	Percent,           // %

	Concat,            // ++
	Repeat,            // **

	Equals,            // ==
	NotEquals,         // !=
	LessThan,          // <
	LessEqual,         // <=
	GreaterThan,       // >
	GreaterEqual,      // >=

	BitwiseAnd,        // &
	BitwiseOr,         // |
	BitwiseXor,        // ^
	LogicalOr,         // ||
	Shl,               // <<
	Shr,               // >>

	PlusWrap,          // +%
	MinusWrap,         // -%
	AsteriskWrap,      // *%

	PlusSaturate,      // +|
	MinusSaturate,     // -|
	AsteriskSaturate,  // *|
	ShlSaturate,       // <<|

	// region Assignment Operators
	PlusAssign,             // +=
	MinusAssign,            // -=
	AsteriskAssign,         // *=
	SlashAssign,            // /=
	PercentAssign,          // %=
	BitwiseAndAssign,       // &=
	BitwiseOrAssign,        // |=
	BitwiseXorAssign,       // ^=
	ShlAssign,              // <<=
	ShrAssign,              // >>=
	PlusWrapAssign,         // +%=
	MinusWrapAssign,        // -%=
	AsteriskWrapAssign,     // *%=
	PlusSaturateAssign,     // +|=
	MinusSaturateAssign,    // -|=
	AsteriskSaturateAssign, // *|=
	ShlSaturateAssign,      // <<|=
	// endregion

	// endregion

	// region Identifiers
	BuiltinIdentifier,
	Identifier,
	BlankIdentifier, // _
	// endregion

	// region Keywords
	AddrspaceKeyword,
	AlignKeyword,
	AllowzeroKeyword,
	AndKeyword,
	AnyframeKeyword,
	AnytypeKeyword,
	AsmKeyword,
	BreakKeyword,
	CallconvKeyword,
	CatchKeyword,
	ComptimeKeyword,
	ConstKeyword,
	ContinueKeyword,
	DeferKeyword,
	ElseKeyword,
	EnumKeyword,
	ErrdeferKeyword,
	ErrorKeyword,
	ExportKeyword,
	ExternKeyword,
	FnKeyword,
	ForKeyword,
	IfKeyword,
	InlineKeyword,
	LinksectionKeyword,
	NoaliasKeyword,
	NoinlineKeyword,
	NosuspendKeyword,
	OpaqueKeyword,
	OrKeyword,
	OrelseKeyword,
	PackedKeyword,
	PubKeyword,
	ResumeKeyword,
	ReturnKeyword,
	StructKeyword,
	SuspendKeyword,
	SwitchKeyword,
	TestKeyword,
	ThreadlocalKeyword,
	TryKeyword,
	UnionKeyword,
	UnreachableKeyword,
	VarKeyword,
	VolatileKeyword,
	WhileKeyword,
	// endregion

	// region Literals
	StringLiteral,
	MultilineStringPart,
	CharLiteral,

	IntegerLiteral,
	FloatLiteral,

	// endregion

	// region Trivia
	Whitespace,
	Newline,

	Comment,
	DocComment,
	TopLevelDocComment,
	// endregion

	ErrorToken;
}

enum class NodeKind : SyntaxKind {
	File,

	// region Declarations
	VarDeclaration,
	PlaceholderDeclaration,
	// endregion

	// region Expressions
	IdentifierExpression,
	LiteralExpression,
	PlaceholderExpression,
	// endregion

	// region Statements
	PlaceholderStatement;
	// endregion
}