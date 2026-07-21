<!--
style: Microsoft Writing Style Guide
reason: Zero-to-PR guide — copy-pasteable commands from clone to first pull request.
doc-type: contributing-guide
audience: New contributors setting up a dev environment for the first time
-->

# Contributing

## 1. Clone

```shell
git clone https://github.com/BratishkaErik/zigocracy
cd zigocracy
```

## 2. Build

```shell
./gradlew build
```

## 3. Run tests

```shell
./gradlew test
```

If you're iterating on one module, you can run just its tests for a faster loop:

```shell
./gradlew ":zon-grammar:test"   # parser only
./gradlew ":cli:test"           # CLI only
```

Run `./gradlew test` before submitting to make sure everything still passes together.

## 4. Submit a pull request

1. Create a feature branch.
2. Make your changes.
3. Run `./gradlew test` and confirm everything passes.
4. Push and open a PR against `main`.

That's it. Welcome!

## Project structure

- **zon-grammar/** — ZON lexer, parser, AST, and token definitions
- **cli/** — main CLI entry point and terminal wrapper
- **meta-grammar-annotations/** — MetaGrammar annotation definitions
- **meta-grammar-processors/** — MetaGrammar annotation processors
- **docs/** — user-facing documentation

<!-- Reference links -->

[docs-root]: docs/
[guide]: docs/zig-object-notation/guide.md
[spec]: docs/zig-object-notation/spec.md
[cli]: docs/cli.md
