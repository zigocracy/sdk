package com.zigocracy.sdk.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.path
import com.zigocracy.sdk.cli.diagnostics.GnuDiagnosticsFormatter
import com.zigocracy.sdk.cli.diagnostics.RichDiagnosticsFormatter
import com.zigocracy.sdk.cli.syntax_highlight.DarkSyntaxHighlightTheme
import com.zigocracy.sdk.cli.syntax_highlight.LightSyntaxHighlightTheme
import com.zigocracy.sdk.zig.parser.Parser
import com.zigocracy.sdk.zig.text.LoadResult
import com.zigocracy.sdk.zig.text.SourceFile
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.walk

internal class CheckSyntaxCommand : CliktCommand(name = "check-syntax") {
	override fun help(context: Context): String =
		"Check Zig source files for syntax errors and report them."

	private val targets by argument(name = "paths")
		.path(mustExist = true, canBeFile = true, canBeDir = true)
		.multiple(required = true)

	private val themeMode by option(
		"--theme",
		help = "Specify terminal visual scheme preference (light or dark)"
	).enum<ThemeMode> { it.name.lowercase() }.default(ThemeMode.Dark)

	enum class ThemeMode { Light, Dark; }

	private val errorFormat by option(
		"--error-style",
		help = "Format for reporting syntax errors (e.g., 'gnu', 'rich', 'rich-5')"
	).convert { input ->
		val normalized = input.lowercase().trim()
		when {
			normalized == "gnu" -> ErrorFormat.Gnu
			normalized == "rich" -> ErrorFormat.Rich(3)
			normalized.startsWith("rich-") -> {
				val sizeStr = normalized.removePrefix("rich-")
				val size = sizeStr.toIntOrNull()
					?: fail("Invalid context size '$sizeStr' for rich format. Expected a number.")
				ErrorFormat.Rich(size)
			}

			else -> fail("Unknown error style '$input'. Expected 'gnu', 'rich', or 'rich-N'.")
		}
	}.default(ErrorFormat.Rich(3))

	sealed class ErrorFormat(val contextSize: Int) {
		object Gnu : ErrorFormat(contextSize = 0)
		class Rich(contextSize: Int) : ErrorFormat(contextSize)
	}

	override fun run() {
		val formatter = when (errorFormat) {
			is ErrorFormat.Gnu -> GnuDiagnosticsFormatter
			is ErrorFormat.Rich -> RichDiagnosticsFormatter(errorFormat.contextSize)
		}

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

		var totalErrors = 0

		for (path in files) {
			when (val loadResult = SourceFile.load(path)) {
				is LoadResult.InvalidExtension -> {
					echo("  ✗ Error: Invalid extension '.${loadResult.extension}'. Expected '.zig'")
					totalErrors++
				}

				is LoadResult.ReadError -> {
					val reason = loadResult.cause.localizedMessage ?: loadResult.cause::class.simpleName ?: "IO error"
					echo("  ✗ Error: Failed to read file ($reason)")
					totalErrors++
				}

				is LoadResult.Success -> {
					try {
						val sourceFile = loadResult.file
						val parserResult = Parser.parseSyntax(sourceFile)

						if (parserResult.stream.diagnostics.isNotEmpty()) {
							totalErrors += parserResult.stream.diagnostics.values.sumOf { it.size }

							formatter.report(terminal, path, parserResult, theme)
						}
					} catch (e: Exception) {
						val reason = e.localizedMessage ?: e::class.simpleName ?: "Unknown failure"
						echo("  ✗ Failed to analyze file '$path' ($reason)")
						totalErrors++
					}
				}
			}
		}

		if (totalErrors > 0) {
			echo("Found $totalErrors errors.")
			throw ProgramResult(1)
		} else {
			echo("All files are syntactically correct!")
		}
	}
}