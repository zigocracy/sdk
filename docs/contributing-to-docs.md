<!--
style: Microsoft Writing Style Guide
reason: Guide for content contributors to help them update text and translations without development tools.
doc-type: contributing-guide
audience: Anyone looking to improve zigocracy text materials, guides, or translations
-->

# Contributing to Documentation

Clear documentation is just as important as reliable code. Accurate
guides and specifications ensure that everyone can understand, use, and
succeed with the Zigocracy toolchain.

You do not need to install any compilers or developer tools to help. All
of our documentation lives in plain-text Markdown (`.md`) files, allowing
you to contribute changes directly using a text editor or the GitHub web
interface.

> [!NOTE]
> Planning to modify toolchain behavior or fix a bug in the code? Please
> follow the [code track][code] instead. If your change updates both code
> and its accompanying documentation, you can see how to structure it in
> the [contribution policy][hub].

## Contribution workflows

Depending on the scale of your planned updates, select the track that
best fits your intent:

| Track                        | Best for | Tools you need |
|------------------------------| --- | --- |
| Option 1: Quick web edits    | Typo fixes, small clarifications, link updates | Only web browser |
| Option 2: Local Git workflow | New guides, restructures, bulk edits | `git` and a text editor |

### Option 1: Quick web edits

Best for small changes, such as fixing a typo, clarifying a sentence, or
updating a link.

Follow the official GitHub guide for editing files directly in your
browser: [Editing files in another user's repository][github-edit-guide].

> [!TIP]
> Small, focused changes are highly appreciated. Keeping one topic per pull
> request makes the review process much faster and easier for everyone.

### Option 2: Local Git workflow

Best for larger changes, such as writing a new guide or restructuring an
existing page.

> [!NOTE]
> This track assumes you have `git` working locally. If you run into any
> trouble setting it up, feel free to switch back to Option 1 and use the web
> interface instead.

If you are new to the pull request workflow, review the
[GitHub Flow guide][github-flow] before you start.

1. Fork the repository on GitHub, then clone your fork to your computer.

```shell
git clone https://github.com/<your-username>/sdk zigocracy-sdk
cd zigocracy-sdk
```

Replace `<your-username>` with your GitHub username before you run the
command.

2. Create a branch for your work.

```shell
git switch --create docs/<your-change>
```

Replace `<your-change>` with a short name for your branch, such as `fix-typo`:

```shell
git switch --create docs/fix-typo
```

3. Write or edit the Markdown files under `docs/`.

4. Stage and commit your changes.

<!--
Note for maintainers: We intentionally hardcode the "docs:" prefix here and
omit wider Conventional Commits specifications to lower the cognitive overhead
for non-developer and translation contributors.
-->

```shell
git add docs/
git commit -m "docs: <describe your change>"
```

Replace `<describe your change>` with a brief summary of your updates,
such as `fix typo in contributing guide`:

```shell
git commit -m "docs: fix typo in contributing guide"
```

5. Push the branch.

```shell
git push -u origin HEAD
```

After pushing, open a pull request on GitHub.

<!-- Reference links -->
[code]: ./contributing-to-code.md
[github-edit-guide]: https://docs.github.com/repositories/working-with-files/managing-files/editing-files#editing-files-in-another-users-repository
[github-flow]: https://docs.github.com/get-started/using-github/github-flow
[hub]: ../CONTRIBUTING.md