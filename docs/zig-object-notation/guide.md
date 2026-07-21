<!--
style: Microsoft Writing Style Guide
reason: Friendly introduction to ZON for developers who have never heard of it — narrative, example-driven, no assumed familiarity.
doc-type: tutorial
audience: Developers unfamiliar with ZON who want to understand what it is and how it works
-->

# ZON — Zig Object Notation

ZON is a data format that comes with the [Zig programming language][zig-zon].
If you have used JSON, TOML, or YAML, you already know the kind of thing it is —
a way to write down structured data that both humans and machines can read.

Zig uses ZON for its own package metadata (`build.zig.zon`), but the format is
general-purpose and can be used for any configuration or data exchange.

## What it looks like

Here is a ZON file that describes a software package:

```zon
.{
    .name = "zlib",
    .version = "1.3.1",
    .enabled = true,
    .sources = .{ "zlib.c", "inflate.c" },
}
```

The outer `.{ }` is a **struct** — ZON's way of grouping values together.
Inside, each line is a field: `.name = "zlib"` sets the field `name` to the
string `"zlib"`.

Structs come in two flavours:

- **Keyed** — named fields (like the example above, similar to JSON objects).
- **Positional** — values listed in order without names (like JSON arrays).

## Values you can write

These are the building blocks.

### Booleans and null

```zon
true
false
null
```

### Integers

Integers can be plain decimal. They can also be hexadecimal (`0xFF`),
octal (`0o644`), or binary (`0b1100`). Underscores are allowed for
readability. There is no size limit.

```zon
42
-7
0xFF
0o644
0b1100
```

### Floats

Floats support decimal and hexadecimal notation, with an optional
scientific exponent:

```zon
3.14
-0.5
1.5e10
0x1.fP3
```

There are also three special float constants:

```zon
nan
inf
-inf
```

### Strings

Strings use double quotes and support escape sequences (`\n`, `\t`, `\x1F`,
`\u{2605}`) just like JSON or C:

```zon
"hello"
"line 1\nline 2"
```

Multi-line strings start with `\\` and run to the end of the line.
There are no inline escapes — the content is literal. Consecutive tokens
are joined with newlines:

```zon
\\First line
\\Second line
\\    Indented third line
```

### Characters

A character is a single Unicode code point written in single quotes.
It supports the same escape sequences as strings:

```zon
'a'
'\t'
'\x7F'
'\u{2605}'
```

### Enum literals

An enum literal is a dot followed by a name — it represents one option from a
fixed set:

```zon
.linux
.ReleaseFast
.@"kebab-case"
```

You will often see them inside structs as field values:

```zon
.{ .build_mode = .ReleaseSafe }
```

If the name contains special characters (hyphens, spaces, and so on), wrap it
in `@"…"` — this is called a **quoted identifier**.

### Structs

A struct groups values together. It is always written as `.` followed by `{ }`.

**Positional** (values in order, acts like an array):

```zon
.{ 1, 2, 3 }
.{ "a", "b", "c" }
.{ 99, }          // trailing comma is fine
```

**Keyed** (named fields, acts like a JSON object):

```zon
.{
    .name = "hello",
    .version = 1,
}
```

An empty struct is just `.{ }`. Structs nest inside each other naturally:

```zon
.{
    .package_name = "example",
    .dependencies = .{
        .lib_a = .{ .url = "https://example.com/pkg.tar.gz" },
    },
}
```

## Comments

Comments start with `//` and go to the end of the line. They can appear
anywhere:

```zon
// A comment on its own line
.{ .x = 1,  // or at the end of a line
   .y = 2 }
```

This is something JSON does not have, and it is surprisingly useful for
configuration files.

## If you already know JSON

ZON fills the same niche as JSON but takes some different design choices.
Here is the same data in both formats:

```json
{"name": "zlib", "version": "1.3.1", "tags": ["data", "compression"]}
```

```zon
.{ .name = "zlib", .version = "1.3.1", .tags = .{ "data", "compression" } }
```

The differences are small but noticeable:

- **Keys without quotes.** Field names are bare identifiers (`.name =`), not
  strings (`"name":`). You only need `@"…"` when the name has special
  characters.
- **No separate array syntax.** JSON has `[]` for arrays and `{}` for objects.
  ZON uses `.{ }` for both — positional elements work like arrays, named
  fields work like objects.
- **More number types.** Hex (`0xFF`), octal (`0o644`), binary (`0b1100`),
  hex floats (`0x1.fP3`), and special constants (`nan`, `inf`).
- **Trailing commas.** Allowed everywhere.
- **Comments.** `//` works anywhere. A small thing that makes a big difference
  in real use.
- **Enum literals.** `.linux`, `.ReleaseSafe` — values with no JSON equivalent.
- **Character literals.** `'a'` — not a single-character string, but a
  separate type representing a Unicode code point as an integer.

Further reading: [Zig documentation: `std.zon`][zig-zon]

<!-- Reference links -->

[zig-zon]: https://ziglang.org/documentation/0.16.0/std/#std.zon
