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

Before building or running the toolchain, ensure **JDK 25**
(Eclipse Temurin or any compatible OpenJDK distribution) is installed.

To run the CLI on your machine, you must currently build it from source.
In the future, a more convenient installation method will be available.

**You will also need a terminal or console (such as bash, zsh, or PowerShell)
open inside the project's main folder so you can run the commands below.**

### Build

As the first step, build the CLI from the root directory:

```shell
./gradlew ":cli:installDist"
```

After running this command, the `zigocracy` launcher should appear in the
`cli/build/install/zigocracy/bin/` folder.

Now that the build step is complete, we can prepare the execution environment.

### Configure the command shortcut

We would love to just run the short `zigocracy` command directly, but
our terminal environment does not know where to find the new executable yet.

Let’s fix this by temporarily registering the `zigocracy` command for
your current terminal window.
Don't worry, this change is strictly temporary and
won't affect any other terminal windows or your global system settings.

If you are on Windows inside PowerShell, run this command:
```powershell
Set-Alias -Name zigocracy -Value "${PWD}\cli\build\install\zigocracy\bin\zigocracy.bat"
```

If you are on Linux or macOS, run this command:
```shell
alias zigocracy="${PWD}/cli/build/install/zigocracy/bin/zigocracy"
```

Now that the shortcut is active, you are ready to run the toolchain.

## Commands

### `check-zon`

This command checks whether your ZON files are well-formed and
highlights any syntax errors.

#### Syntax

```shell
zigocracy check-zon <paths>...
```

#### Examples

Check a single file:
```shell
zigocracy check-zon build.zon
```

Check an entire folder recursively:
```shell
zigocracy check-zon ./src
```

Check multiple files and folders at once:
```shell
zigocracy check-zon package.zon build.zon ./internal/config
```

#### File validation

Here is how a successful syntax check looks:

```text
─── build.zon ───
  ✓ Valid ZON
```

If the parser encounters a syntax error, it points directly to the issue:

```text
─── libs/zap/build.zon ───
  ✗ Expected '}', got ''
    at line 12, column 5
    author = "zig-team"
    ^── here
```

#### Multi-file summaries

If the command evaluates more than one file, it appends a brief summary
at the bottom to show the overall results.

```text
─── Summary ───
  ✓ 2 files: 1 passed, 1 failed
```

#### Exit codes

* `0` — Success. The tool validated all files, and found no syntax errors.
* `1` — Syntax error. One or more files contain broken or invalid ZON syntax.
* `2` — No files found. Scanned paths contain no `.zon` files to validate.

