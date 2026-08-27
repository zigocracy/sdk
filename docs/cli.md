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

### `highlight-syntax`

Print Zig source files with full syntax highlighting like a cat tool.

#### Syntax

```shell
zigocracy highlight-syntax [OPTIONS] <paths>...
```

#### Options

* `--theme` — Specify terminal visual scheme preference (`light` or `dark`). Default: `dark`.

#### Examples

Highlight a single file:

```shell
zigocracy highlight-syntax main.zig
```

Highlight multiple files and directories at once:

```shell
zigocracy highlight-syntax src/ math.zig build.zig
```

Force a specific theme configuration:

```shell
zigocracy highlight-syntax --theme=light main.zig
```

#### Exit codes

* `0` — Success. The tool processed and printed all specified Zig files.
* `2` — No files found. Scanned paths contain no `.zig` files to process.

### `check-syntax`

Check Zig source files for syntax errors and report them.

#### Syntax

```shell
zigocracy check-syntax [OPTIONS] <paths>...
```

#### Options

* `--theme` — Specify terminal visual scheme preference (`light` or `dark`). Default: `dark`.
* `--error-style` — Format for reporting syntax errors (`gnu`, `rich`, or `rich-<N>` where `<N>` is the context line count). Default: `rich` (evaluates to 3 lines of context).

#### Examples

Check a single file with default reporting options:

```shell
zigocracy check-syntax main.zig
```

Scan an entire directory recursively:

```shell
zigocracy check-syntax src/
```

Print compiler diagnostics in the standard single-line GNU format:

```shell
zigocracy check-syntax --error-style=gnu src/
```

Change the number of surrounding code lines displayed for each error:

```shell
zigocracy check-syntax --error-style=rich-5 src/
```

#### Exit codes

* `0` — Success. All analyzed files are syntactically correct.
* `1` — Syntax error. One or more files contain invalid syntax or failed processing.
* `2` — No files found. Scanned paths contain no `.zig` files to process.

### `lsp-server`

Start the Language Server Protocol (LSP) server for IDE and text editor integration.

This server acts as the brains behind your development environment. It runs in the
background and talks directly to your IDE or editor to provide smart features like
code autocompletion, real-time error highlighting, and instant code navigation.

> [!NOTE]
> Usually, an editor plugin with Zigocracy support will launch and manage this
> server automatically. You can run this command to set up the connection
> manually if a compatible plugin is not available, or if your preferred
> editor requires manual LSP configuration.

> [!IMPORTANT]
> Because integration plugins do not exist yet, this manual connection is
> currently the only way to use the server.

#### Syntax

```shell
zigocracy lsp-server [OPTIONS]
```

#### Options

##### Server Options

* `--port=<PORT>` — Port to listen on via TCP. If omitted, standard I/O (stdio) transport is used.

##### Debugging Options

* `--validate` — Enable strict validation of incoming messages.
* `--trace` — Log raw server traffic to help troubleshoot integration issues.

#### Examples

Start the server using standard I/O (stdio) transport:

```shell
zigocracy lsp-server
```

Start the server using TCP transport listening on a specific port:

```shell
zigocracy lsp-server --port 5444
```

Start debug server, using TCP transport, with strict validation and full traffic logging:

```shell
zigocracy lsp-server --port 5444 --trace --validate
```

#### Server logs

Here is what you will see in the terminal when launching the server in `stdio` mode
(this mode uses text input and output to talk to your editor):

```text
Starting LSP server via standard I/O (stdio)...
LSP server is actively listening for stdio messages.
```

When you start the server using TCP, it explicitly states that it is waiting for a client connection. The session activates as soon as the IDE or text editor connects:

```text
Starting LSP server on TCP port 5444 (waiting for client connection...)
Client connected from /127.0.0.1:53241
LSP session established. Processing client requests...
```

#### Exit codes

#### Exit codes

* `0` — Success. The server stopped cleanly after the editor ended the session.
* `1` — Startup or connection error. The server failed to bind to the port, lost connection, or the protocol sequence was broken.
* `2` — Internal crash. The server engine encountered an unexpected runtime failure or failed to process messages.