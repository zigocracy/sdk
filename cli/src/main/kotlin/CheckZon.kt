package com.zigocracy.sdk.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.types.path
import com.zigocracy.sdk.zon.*
import java.nio.file.Files
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.walk

internal class CheckZon : CliktCommand(name = "check-zon") {
	override fun help(context: Context): String =
		"Verify syntax of Zig Object Notation (ZON) files."

	private val targets by argument(name = "paths", help = "The ZON files or directories to verify")
		.path(mustExist = true, canBeFile = true, canBeDir = true)
		.multiple(required = true)

	override fun run() {
		val files = targets.flatMap { path ->
			try {
				if (path.isDirectory()) {
					path.walk().filter {
						it.isRegularFile() && it.extension.equals("zon", ignoreCase = true)
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
			echo("No .zon files found in the provided paths")
			throw ProgramResult(2)
		}

		var passed = 0
		var failed = 0

		for (path in files) {
			val displayPath = path.normalize().toString()
			echo("─── $displayPath ───")
			val source = Files.readString(path)

			when (val lexResult = ZonLexer(source).tokenize()) {
				is LexResult.Error -> {
					renderError(lexResult.diagnostic)
					failed++
				}

				is LexResult.Success -> {
					when (val parseResult = ZonParser(lexResult.tokens, source).parse()) {
						is ParseResult.Error -> {
							renderError(parseResult.diagnostic)
							failed++
						}

						is ParseResult.Success -> {
							echo("  ✓ Valid ZON")
							passed++
						}
					}
				}
			}
			echo("")
		}

		if (files.size > 1) {
			val total = passed + failed
			echo("─── Summary ───")
			if (failed == 0) {
				echo("  ✓ All $total file(s) valid")
			} else {
				echo("  ✓ $total files: $passed passed, $failed failed")
			}
		}

		if (failed > 0) throw ProgramResult(1)
	}

	private fun renderError(d: Diagnostic) {
		echo("  ✗ ${d.message}")
		echo("    at line ${d.location.line}, column ${d.location.column}")
		if (d.sourceLine.isNotBlank()) {
			echo("    ${d.sourceLine}")
			val caret = " ".repeat((d.location.column - 1).coerceAtLeast(0)) + "^── here"
			echo("    $caret")
		}
	}
}
