<!--
style: Microsoft Writing Style Guide
reason: Root contribution hub to direct users to their specialized track without friction.
doc-type: hub
audience: Anyone looking to contribute to the zigocracy project for the first time
-->

# Contributing to zigocracy

Welcome! We are glad you are here. No matter which part of the project you
want to improve, your time and contributions are highly valued.

To keep our instructions clear and easy to follow, we have organized our
workflow into two specialized tracks. Please choose the path that best
fits what you want to do:

* **[Contributing to Code](docs/contributing-to-code.md)** — Go here if
  you want to make changes to how the toolchain works.
* **[Contributing to Documentation](docs/contributing-to-docs.md)** — Go here if
  you want to help with the project documentation, guides, or translations.

Thank you for helping shape the future of zigocracy!

Welcome! We are excited to have you here. To give you the best experience,
we have separated our guides based on how you want to contribute:

* **Looking to improve our guides or fix typos?** Please jump straight to
  our [Documentation Guide](docs/contributing.md). You won't
  need to install any compilers or developer tools!
* **Looking to hack on the toolchain or core tooling?** You are in the right
  place. Review the core expectations and setup guide below.

## Core Code Expectations

### 1. The Language
Even though `zigocracy` serves the Zig ecosystem, the core project is
written in Kotlin. While you will be writing Kotlin most of the time,
we rely on Java libraries under the hood, so a basic ability to read Java
syntax is highly recommended.

### 2. The Build System
For core development, we use **Gradle** instead of the native `zig build`
system. You will need a working Java Development Kit (JDK) installed on
your machine to compile code and run tests.

## Getting Started

If you are ready to get your hands dirty, follow this zero-to-PR guide
to set up your environment and submit your first change.

## 1. Clone the repository

```shell
git clone https://github.com/BratishkaErik/zigocracy
cd zigocracy
```

## 2. Build the project

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
