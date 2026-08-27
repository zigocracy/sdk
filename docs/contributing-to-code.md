<!--
style: Microsoft Writing Style Guide
reason: Zero-to-PR guide for code contributors — copyable commands from first clone to pull request.
doc-type: contributing-guide
audience: Developers looking to make changes to the Zigocracy toolchain
-->

# Contributing to Code

This guide will help you set up your environment, build the project, and
submit your first code change.

> [!NOTE]
> If you are here to fix typos, improve guides, or update translations, you
> don't need a local development environment. Save your time and switch to
> the [documentation track][docs]. For mixed pull requests, check out the
> [contribution policy][hub].

## Core expectations

### The language

Even though Zigocracy serves the Zig ecosystem, the core project is
written in Kotlin. You will be writing Kotlin most of the time, and we
rely on Java libraries under the hood, so a basic ability to read Java
syntax is highly recommended.

### The build system

For core development, we use **Gradle** instead of the native `zig build`
system. You will need a working Java Development Kit (JDK) installed on
your machine to compile code and run tests. You do not need to install
the Zig compiler itself.

## Prerequisites

- JDK 25 — Eclipse Temurin or any compatible OpenJDK distribution
- `git`
- A terminal, such as bash, zsh, or PowerShell

## Set up your development environment

### Fork and clone

Fork the repository on GitHub, then clone your fork to your computer.

```shell
git clone https://github.com/<your-username>/sdk zigocracy-sdk
cd zigocracy-sdk
```

Replace `<your-username>` with your GitHub username before you run the
command.

### Create a branch

Create a branch for your work.

```shell
git switch --create <your-change>
```

Replace `<your-change>` with a short name for your branch, such as `fix/zon-parser-crash`:

```shell
git switch --create fix/zon-parser-crash
```

## Build the project

Build the project from the root directory:

```shell
./gradlew build
```

## Run checks

Run the full check suite:

```shell
./gradlew check
```

If you are iterating on one module, run just its checks for a faster
loop:

```shell
./gradlew ":zig-grammar:check"  # Run tests for the parser component
./gradlew ":cli:check" # Run tests for the main CLI wrapper
```

Run `./gradlew check` before submitting to make sure everything still
passes together.

## Submit a pull request

If you are new to the pull request workflow, review the
[GitHub Flow guide][github-flow] before you start.

1. Stage and commit your changes.

```shell
git add .
git commit -m "your message"
```

Write a short message describing your change. Our project uses the
[Conventional Commits][cc] format to keep the repository history clean and structured.
If you are familiar with this style, we highly recommend using it for your
commits. If you are not, please don't let that stop you — simply write a plain
summary of your updates, and we will gladly help you figure out the proper
format and title together during the review process.

Here are a few examples of how to format it:

- With Conventional Commits:
  ```shell
  git commit -m "fix(parser): prevent crash on trailing commas in ZON"
  ```
- With a casual description:
  ```shell
  git commit -m "Fix ZON parser crash"
  ```

2. Push the branch.

```shell
git push -u origin HEAD
```

After pushing, open a pull request on GitHub.

> [!NOTE]
> It is always a good idea to run `./gradlew check` before pushing.
> Catching errors early saves you from needing to push new commits
> just to fix minor overlooked mistakes. It also helps our maintainers
> review and accept your changes much faster.

<!-- Reference links -->
[cc]: https://www.conventionalcommits.org/
[docs]: ./contributing-to-docs.md
[github-flow]: https://docs.github.com/get-started/using-github/github-flow
[hub]: ../CONTRIBUTING.md
