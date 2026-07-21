<!--
style: Microsoft Writing Style Guide
reason: Onboarding document for new contributors — task-oriented, minimal prose, commands-first.
doc-type: contributing-guide
audience: Developers who want to build, test, and contribute to the project
-->

# Contributing

## Build

```shell
./gradlew build
```

## Test

```shell
# All tests
./gradlew test

# Just the parser
./gradlew ":zon-grammar:test"

# Just the CLI
./gradlew ":cli:test"
```

## Validate a ZON file

```shell
./gradlew ":cli:run" --args="path/to/file.zon"
./gradlew ":cli:run" --args="path/to/directory"
```

## Documentation

Docs live in [docs/][docs-root]:

- [ZON guide][guide] — tutorial for newcomers
- [ZON specification][spec] — formal spec
- [CLI reference][cli] — command usage and output

## Modules

- [zon-grammar/][zp] — lexer, parser, AST, token definitions
- [meta-grammar-annotations/][mga] — annotation markers (`@Keyword`, `@Operator`, etc.)
- [meta-grammar-processors/][mgp] — compile-time code generators
- [cli/][cli-mod] — command-line interface

<!-- Reference links -->

[docs-root]: docs/
[guide]: docs/zig-object-notation/guide.md
[spec]: docs/zig-object-notation/spec.md
[cli]: docs/cli.md
[zp]: zon-grammar/
[mga]: meta-grammar-annotations/
[mgp]: meta-grammar-processors/
[cli-mod]: cli/
