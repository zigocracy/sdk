<!--
style: Google Developer Documentation Style Guide
reason: Formal specification of the ZON data format — precise, structured, for implementers and tooling authors.
doc-type: specification
audience: Developers implementing ZON parsers, validators, or serializers
-->

# ZON specification

**Version:** 1.0 (informal)
**Based on:** Zig's `std.zon` as of Zig 0.16

ZON (Zig Object Notation) is a text format for structured data. A ZON file
contains a single expression encoded as UTF-8 text.

## 1. Grammar

Syntax is described in EBNF:

```
(* Top-level *)
start       = expr ;

(* Expressions *)
expr        = struct_init | enum_literal
            | string | multiline_string
            | number | bool | null | char_literal ;

(* Structs *)
struct_init = "." "{" struct_body "}" | "." "{" "}" ;
struct_body = keyed_body | positional_body ;
keyed_body  = field_init ("," field_init)* ","? ;
positional_body = expr ("," expr)* ","? ;
field_init  = "." identifier "=" expr ;

(* Named values *)
enum_literal = "." identifier ;
identifier  = PLAIN_ID | QUOTED_ID ;

(* Text values *)
string          = STRING_SINGLE ;
multiline_string = STRING_MULTI+ ;

(* Scalar values *)
bool        = "true" | "false" ;
null        = "null" ;
char_literal = CHAR_LIT ;

(* Numeric values *)
number      = FLOAT_LIT | INT_LIT
            | "-" FLOAT_LIT | "-" INT_LIT
            | "nan" | "inf" | "-" "inf" ;
```

A struct body is **keyed** if it begins with `.` followed by an identifier then
`=`. Otherwise it is **positional**. Mixing named and positional elements in
the same struct is not allowed.

## 2. Lexical elements

### 2.1 Whitespace and comments

Whitespace (spaces `U+0020`, tabs `U+0009`, newlines `U+000A`) separates
tokens and is otherwise ignored.

Comments begin with `//` and extend to the end of the line (`U+000A` or EOF).
They are treated as whitespace.

### 2.2 Identifiers

```
PLAIN_ID  = [a-zA-Z_] [a-zA-Z0-9_]* ;
QUOTED_ID = "@" STRING_SINGLE ;
```

A plain identifier matches the regex `[a-zA-Z_][a-zA-Z0-9_]*`. A quoted
identifier is an `@` followed by a string literal — the string is unescaped
to produce the identifier value.

### 2.3 Strings and characters

```
STRING_SINGLE  = '"' (escape | [^"\\])* '"' ;
STRING_MULTI   = "\\\\" [^\n]* ;
CHAR_LIT       = "'" (escape | [^'\\]) "'" ;
```

`STRING_MULTI` starts with `\\` and captures the rest of the line as literal
text — no escape processing is applied. Consecutive `STRING_MULTI` tokens
are joined with newlines.

#### Escape sequences

Valid inside `STRING_SINGLE` and `CHAR_LIT`:

| Sequence | Code point |
|---|---|
| `\n` | U+000A (newline) |
| `\r` | U+000D (carriage return) |
| `\t` | U+0009 (tab) |
| `\\` | U+005C (backslash) |
| `\'` | U+0027 (single quote) |
| `\"` | U+0022 (double quote) |
| `\xNN` | U+0000–U+00FF (2 hex digits) |
| `\u{NN…}` | U+0000–U+10FFFF (1+ hex digits in braces) |

Unrecognised escape sequences are kept as-is.

### 2.4 Numbers

#### Integers

```
INT_LIT    = HEX_INT | OCT_INT | BIN_INT | DEC_INT ;
HEX_INT    = "0x" | "0X" hex_digit (hex_digit | "_")* ;
OCT_INT    = "0o" | "0O" oct_digit (oct_digit | "_")* ;
BIN_INT    = "0b" | "0B" bin_digit (bin_digit | "_")* ;
DEC_INT    = dec_digit (dec_digit | "_")* ;
```

Where: `hex_digit ∈ [0-9a-fA-F]`, `oct_digit ∈ [0-7]`,
`bin_digit ∈ {0, 1}`, `dec_digit ∈ [0-9]`.

#### Floats

```
FLOAT_LIT  = HEX_FLOAT_FRAC | HEX_FLOAT_EXP
           | DEC_FLOAT_FRAC | DEC_FLOAT_EXP ;

HEX_FLOAT_FRAC = ("0x" | "0X") hex_digit+ "." hex_digit+ exponent_hex? ;
HEX_FLOAT_EXP  = ("0x" | "0X") hex_digit+ exponent_hex ;
DEC_FLOAT_FRAC = dec_digit+ "." dec_digit+ exponent_dec? ;
DEC_FLOAT_EXP  = dec_digit+ exponent_dec ;

exponent_hex = ("p" | "P") ("-" | "+")? dec_digit+ ;
exponent_dec = ("e" | "E") ("-" | "+")? dec_digit+ ;
```

Underscores (`_`) are allowed wherever a digit is expected in any numeric
literal and are ignored during parsing.

### 2.5 Punctuation and operators

- `.` — Dot
- `{` — Left brace
- `}` — Right brace
- `,` — Comma
- `=` — Equals
- `@` — At (quoted identifier prefix)
- `-` — Minus (prefix negation)

### 2.6 Keywords

The following words are reserved keywords and cannot be used as plain
identifiers:

```
true   false   null   nan   inf
```

## 3. Value types

Each valid expression evaluates to one of the following types.

### Boolean

`true` and `false`.

### Null

`null` — absence of a value.

### Integer

An arbitrary-precision whole number. May be negated with the `-` prefix.
`-0` is valid.

### Float

An IEEE 754 floating-point value. Three special values exist:

| Expression | IEEE 754 value |
|---|---|
| `nan` | Not-a-number (NaN) |
| `inf` | +∞ |
| `-inf` | −∞ |

### String

A sequence of Unicode code points. Escape sequences (see §2.3) are processed
during parsing. Multi-line strings (`STRING_MULTI`) are literal — no escape
processing.

### Character

A single Unicode code point, represented as its numeric value.
Supports the same escape sequences as strings.

### Enum literal

A dot (`.`) followed by an identifier. A self-contained value with no
associated data, typically used to represent a choice from a fixed set.

```
.linux   .ReleaseFast   .@"kebab-case"
```

### Struct

A container of multiple values enclosed in `.{ }`:

- **Positional** (array-like): values listed in order, comma-separated.
  Trailing commas are allowed: `.{ 1, 2, 3, }`.
- **Keyed** (object-like): each value is preceded by `.name = `.
  The name is an identifier (plain or quoted).

An empty struct is `.{ }`.

## 4. Implementation guidance

### Parsing order

A recommended parse pipeline:

1. **Lex**: scan the source text into a token stream. Skip whitespace and
   comments. Match keywords before identifiers (keywords are reserved).
2. **Parse**: consume tokens according to the grammar in §1. Use recursive
   descent or a comparable algorithm.
3. **Unescape**: process escape sequences in strings, characters, and quoted
   identifiers.
4. **Convert**: parse numeric literals — detect base from prefix, strip
   underscores, convert to the target numeric representation.

### Error reporting

Errors should include:

- Byte offset from the start of input
- Line number (1-based) and column number (1-based)
- A message describing the problem
- The offending source line (when available)

Common error conditions:

- **Unexpected character** — character does not start any valid token.
- **Unexpected token** — token does not follow the grammar at this position.
- **Expected expression** — a value was required but not found.
- **Expected identifier** — an identifier was required but not found.
- **`-nan`** — must use `nan` alone; `-nan` is not a valid literal.

### Known limitations

- Quoted identifiers (`@"…"`) that contain null bytes (`U+0000`) should be
  rejected by a conforming parser.
- Mixing keyed and positional elements in a single struct is a syntax error.
- The expression `-nan` is not valid — use `nan` for the NaN value.

## 5. References

See the [Zig documentation][zig-docs].

<!-- Reference links -->

[zig-docs]: https://ziglang.org/documentation/0.16.0/std/#std.zon
