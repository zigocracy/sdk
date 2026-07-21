<!--
style: Microsoft Writing Style Guide
reason: Command-line interface reference — describes all available CLI commands.
doc-type: reference
audience: Users who run the CLI from a terminal
-->

# CLI reference

> [!IMPORTANT]
> **Alpha Status:** This project is currently under active development.
> This document is intended for alpha users, contributors, and early adopters.

## Prerequisites

Before building or running the toolchain, ensure you have the following
installed:
* **JDK 25** (Eclipse Temurin or any compatible OpenJDK distribution).

Currently, you must build the CLI from source and run it directly from the
build output. This process will be streamlined in future releases.

## Build

```shell
./gradlew ":cli:installDist"
```

After building, the launcher script is at `cli/build/install/cli/bin/cli`
(or `cli.bat` on Windows).

## Commands

### `check-zon`

Validate ZON files and report any issues with line numbers and descriptions.

```shell
cli/build/install/cli/bin/cli path/to/file.zon
cli/build/install/cli/bin/cli path/to/directory
```

Output:

```text
─── bad.zon ───
  ✗ Expected '}', got ''
    at line 2, column 1

─── good.zon ───
  ✓ Valid ZON

─── Summary ───
  ✓ 3 files: 2 passed, 1 failed
```

**Input** — File path or directory path. Directories are walked recursively
for `.zon` files.

**Exit code** — 0 if all files are valid, 1 if any file has errors.
