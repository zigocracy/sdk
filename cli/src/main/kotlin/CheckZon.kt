package net.landless_city.zigocracy.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import net.landless_city.zigocracy.zon.*
import java.nio.file.Files
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.walk

internal class CheckZon : CliktCommand() {
	private val target by argument()
		.file(mustExist = true, canBeFile = true, canBeDir = true)

	override fun run() {
		val root = target.toPath()
		val files = if (root.isDirectory()) {
			root.walk().filter {
				it.isRegularFile() && it.extension.equals("zon", ignoreCase = true)
			}.toList()
		} else {
			listOf(root)
		}

		if (files.isEmpty()) {
			echo("No .zon files found in $target")
			throw ProgramResult(1)
		}

		var passed = 0
		var failed = 0

		for (path in files) {
			echo("─── ${path.fileName} ───")
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
