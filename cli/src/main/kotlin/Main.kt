package com.zigocracy.sdk.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands

public class Zigocracy : NoOpCliktCommand() {
	override fun help(context: Context): String =
		"An alternative Zig compiler and toolchain."

	override fun helpEpilog(context: Context): String = """
		Important: This toolchain is a work in progress and is not yet ready for regular use.
		This means current commands can change or break at any time without warning.

		If you think you found a bug, you can report it here: 
		https://github.com/zigocracy/sdk/issues

		Also, if you want to ask questions, discuss things with others, or give feedback, you are always welcome in our open forum: 
		https://github.com/zigocracy/sdk/discussions

		Thank you for trying out Zigocracy. We really hope it will make your development experience smooth and rewarding."""
		.trimIndent().replace("\n", "\u0085")
}

public fun main(args: Array<String>) =
	Zigocracy()
		.subcommands(CheckZon(), CheckSyntaxCommand(), HighlightSyntaxCommand(), LspServerCommand())
		.main(args)