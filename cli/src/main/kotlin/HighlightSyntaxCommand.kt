package com.zigocracy.sdk.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.path
import com.zigocracy.sdk.cli.syntax_highlight.DarkSyntaxHighlightTheme
import com.zigocracy.sdk.cli.syntax_highlight.HighlightPrinter
import com.zigocracy.sdk.cli.syntax_highlight.LightSyntaxHighlightTheme
import com.zigocracy.sdk.zig.parser.Parser
import com.zigocracy.sdk.zig.syntax.traverseFromRoot
import com.zigocracy.sdk.zig.text.LoadResult
import com.zigocracy.sdk.zig.text.SourceFile
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.walk

internal class HighlightSyntaxCommand : CliktCommand(name = "highlight-syntax") {
	override fun help(context: Context): String =
		"Print Zig source files with full syntax highlighting like a cat tool."

	private val targets by argument(name = "paths", help = "The Zig files or directories to highlight")
		.path(mustExist = true, canBeFile = true, canBeDir = true)
		.multiple(required = true)

	private val themeMode by option(
		"--theme",
		help = "Specify terminal visual scheme preference (light or dark)"
	).enum<ThemeMode> { it.name.lowercase() }.default(ThemeMode.Dark)

	enum class ThemeMode { Light, Dark }

	override fun run() {
		val theme = when (themeMode) {
			ThemeMode.Dark -> DarkSyntaxHighlightTheme
			ThemeMode.Light -> LightSyntaxHighlightTheme
		}

		val files = targets.flatMap { path ->
			try {
				if (path.isDirectory()) {
					path.walk().filter {
						it.isRegularFile() && it.extension.equals("zig", ignoreCase = true)
					}.toList()
				} else {
					listOf(path)
				}
			} catch (e: java.nio.file.FileSystemException) {
				val reason = when (e) {
					is java.nio.file.AccessDeniedException -> "Access denied"
					is java.nio.file.NoSuchFileException -> "No such file or directory"
					is java.nio.file.FileSystemLoopException -> "Circular file system loop detected"
					else -> "File system error"
				}
				echo("⚠ Warning: Cannot scan path '$path' ($reason)")
				emptyList()
			} catch (e: Exception) {
				val reason = e.localizedMessage ?: e::class.simpleName ?: "Unknown error"
				echo("⚠ Warning: Unexpected failure while scanning '$path' ($reason)")
				emptyList()
			}
		}
			.distinctBy { it.toAbsolutePath().normalize().toString() }

		if (files.isEmpty()) {
			echo("No .zig files found in the provided paths")
			throw ProgramResult(2)
		}

		for (path in files) {
			when (val loadResult = SourceFile.load(path)) {
				is LoadResult.InvalidExtension -> {
					echo("  ✗ Error: Invalid extension '.${loadResult.extension}'. Expected '.zig'")
				}

				is LoadResult.ReadError -> {
					val reason = loadResult.cause.localizedMessage ?: loadResult.cause::class.simpleName ?: "IO error"
					echo("  ✗ Error: Failed to read file ($reason)")
				}

				is LoadResult.Success -> {
					try {
						val parserResult = Parser.parseSyntax(loadResult.file)

						val highlighter = HighlightPrinter(
							terminal = terminal,
							sourceFile = parserResult.source,
							theme = theme
						)

						parserResult.stream.traverseFromRoot(highlighter)
					} catch (e: Exception) {
						val reason = e.localizedMessage ?: e::class.simpleName ?: "Unknown failure"
						echo("  ✗ Failed to analyze file ($reason)")
					}
				}
			}
		}
	}
}