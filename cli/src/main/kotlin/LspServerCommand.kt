package com.zigocracy.sdk.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.terminal.danger
import com.github.ajalt.mordant.terminal.success
import com.zigocracy.sdk.lsp.lifecycle.LspConnectionLostException
import com.zigocracy.sdk.lsp.lifecycle.LspProtocolException
import com.zigocracy.sdk.lsp.lifecycle.LspServerRunner

internal class LspServerCommand : CliktCommand(name = "lsp-server") {
	override fun help(context: Context): String =
		"Start the Language Server Protocol (LSP) server."

	override fun helpEpilog(context: Context): String = """
		Examples:
		  zigocracy lsp-server
		  zigocracy lsp-server --port 5444"""
		.trimIndent().replace("\n", "\u0085")

	private val serverOptions by ServerOptions()

	private val debug by DebugOptions()

	override fun run() {
		val port = serverOptions.port
		val isTcp = port != null

		val runner = LspServerRunner(
			isTcp = isTcp,
			port = port ?: 0,
			validate = debug.validate,
			trace = debug.trace
		)

		val session = try {
			runner.start()
		} catch (e: Exception) {
			terminal.danger("LSP server startup failed: ${e.localizedMessage ?: e::class.simpleName}", stderr = true)
			throw ProgramResult(1)
		}

		val exitCode = try {
			session.serve()
			terminal.success("LSP server exited cleanly.", stderr = true)
			0
		} catch (e: LspProtocolException) {
			terminal.danger("LSP server exited with protocol error: ${e.message}", stderr = true)
			1
		} catch (e: LspConnectionLostException) {
			terminal.danger("LSP server connection lost: ${e.message}", stderr = true)
			1
		} catch (e: Exception) {
			terminal.danger("LSP server crashed internally: ${e.localizedMessage ?: e::class.simpleName}", stderr = true)
			2
		}

		throw ProgramResult(exitCode)
	}
}

private class ServerOptions : OptionGroup(name = "Server Options:") {
	val port by option(
		"--port",
		metavar = "PORT",
		help = "Port to listen on via TCP. If omitted, \"stdio\" transport will be used."
	).int()
}

private class DebugOptions : OptionGroup(name = "Debugging Options:") {
	val validate by option(
		"--validate",
		help = "Enable strict validation of incoming messages."
	).flag(default = false)

	val trace by option(
		"--trace",
		help = "Log raw server traffic to help troubleshoot integration issues."
	).flag(default = false)
}