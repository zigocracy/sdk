<!--
style: Microsoft Writing Style Guide
reason: Per-command reference organised by usage — syntax, flags, examples.
doc-type: reference
audience: Users running the CLI who need to find and apply the right command
-->

# CLI reference

> [!IMPORTANT]
> **Alpha Status:** This project is currently under active development.
> This document is intended for alpha users, contributors, and early adopters.

## Prerequisites

Before building or running the toolchain, ensure **JDK 25** (Eclipse Temurin or
any compatible OpenJDK distribution) is installed.

You must build the CLI from source and run it directly from the build output.
This will be streamlined in future releases.

## Build

```shell
./gradlew ":cli:installDist"
```

After building, the launcher script is at `cli/build/install/cli/bin/cli`
(or `cli.bat` on Windows).

## Commands

### `check-zon`

Validate ZON files and report issues.

```shell
cli/build/install/cli/bin/cli path/to/file.zon
cli/build/install/cli/bin/cli path/to/directory
```

**Output**

```text
─── bad.zon ───
  ✗ Expected '}', got ''
    at line 2, column 1

─── good.zon ───
  ✓ Valid ZON

─── Summary ───
  ✓ 3 files: 2 passed, 1 failed
```

**Input** — a file path or a directory path. Directories are walked
recursively for `.zon` files.

**Exit code** — 0 if all files are valid, 1 if any file has errors.

**Use when** — you want to check whether a ZON file is well-formed before
using it in a pipeline or committing changes.
