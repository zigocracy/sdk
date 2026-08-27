package net.landless_city.zigocracy.zig.syntax

import net.landless_city.zigocracy.zig.syntax.TokenKind.*

enum class VisualGroup {
	Whitespace,
	Newline,
	BadCharacter,
	Keyword,
	BuiltinIdentifier,
	Identifier,
	Operator,
	Number,
	String,
	Comment,
	DocComment,
	Punctuation;
}

fun TokenKind.classifyToVisualGroup(): VisualGroup = when (this) {
	Whitespace -> VisualGroup.Whitespace
	Newline -> VisualGroup.Newline

	ErrorToken -> VisualGroup.BadCharacter

	BuiltinIdentifier -> VisualGroup.BuiltinIdentifier

	Identifier, BlankIdentifier -> VisualGroup.Identifier

	Comment -> VisualGroup.Comment

	DocComment, TopLevelDocComment -> VisualGroup.DocComment

	IntegerLiteral, FloatLiteral -> VisualGroup.Number

	StringLiteral, MultilineStringPart, CharLiteral -> VisualGroup.String

	LeftParen, RightParen, LeftBrace, RightBrace,
	LeftBracket, RightBracket, Comma, Semicolon,
	Colon, Dot, Range, Ellipsis -> VisualGroup.Punctuation

	Assign, FatArrow, SkinnyArrow, QuestionMark,
	ExclamationMark, Tilde, PtrDereference, OptionalUnwrap,
	Plus, Minus, Asterisk, Slash, Percent,
	Concat, Repeat, Equals, NotEquals, LessThan,
	LessEqual, GreaterThan, GreaterEqual, BitwiseAnd,
	BitwiseOr, BitwiseXor, LogicalOr, Shl, Shr,
	PlusWrap, MinusWrap, AsteriskWrap, PlusSaturate,
	MinusSaturate, AsteriskSaturate, ShlSaturate, PlusAssign,
	MinusAssign, AsteriskAssign, SlashAssign, PercentAssign,
	BitwiseAndAssign, BitwiseOrAssign, BitwiseXorAssign, ShlAssign,
	ShrAssign, PlusWrapAssign, MinusWrapAssign, AsteriskWrapAssign,
	PlusSaturateAssign, MinusSaturateAssign, AsteriskSaturateAssign,
	ShlSaturateAssign -> VisualGroup.Operator

	AddrspaceKeyword, AlignKeyword, AllowzeroKeyword, AndKeyword,
	AnyframeKeyword, AnytypeKeyword, AsmKeyword, BreakKeyword,
	CallconvKeyword, CatchKeyword, ComptimeKeyword, ConstKeyword,
	ContinueKeyword, DeferKeyword, ElseKeyword, EnumKeyword,
	ErrdeferKeyword, ErrorKeyword, ExportKeyword, ExternKeyword,
	FnKeyword, ForKeyword, IfKeyword, InlineKeyword,
	LinksectionKeyword, NoaliasKeyword, NoinlineKeyword, NosuspendKeyword,
	OpaqueKeyword, OrKeyword, OrelseKeyword, PackedKeyword,
	PubKeyword, ResumeKeyword, ReturnKeyword, StructKeyword,
	SuspendKeyword, SwitchKeyword, TestKeyword, ThreadlocalKeyword,
	TryKeyword, UnionKeyword, UnreachableKeyword, VarKeyword,
	VolatileKeyword, WhileKeyword -> VisualGroup.Keyword
}