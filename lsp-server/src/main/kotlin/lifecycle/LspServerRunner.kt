package com.zigocracy.sdk.lsp.lifecycle

import com.zigocracy.sdk.lsp.server.ZigocracyLanguageServer
import org.eclipse.lsp4j.launch.LSPLauncher
import java.io.PrintWriter
import java.net.ServerSocket
import java.util.concurrent.CompletableFuture

class LspServerRunner(
	private val isTcp: Boolean,
	private val port: Int,
	private val validate: Boolean = false,
	private val trace: Boolean = false
) {
	fun start(): LspSession {
		val exitFuture = CompletableFuture<Boolean>()
		val traceWriter = if (trace) PrintWriter(System.err, true) else null

		lateinit var session: LspSession

		val server = ZigocracyLanguageServer(onExit = { isNormalShutdown ->
			session.close(isNormalShutdown)
		})

		session = if (isTcp) {
			setupTcpTransport(server, exitFuture, traceWriter)
		} else {
			setupStdioTransport(server, exitFuture, traceWriter)
		}

		return session
	}

	private fun setupTcpTransport(
		server: ZigocracyLanguageServer,
		exitFuture: CompletableFuture<Boolean>,
		traceWriter: PrintWriter?
	): LspSession {
		System.err.println("Starting LSP server on TCP port $port (waiting for client connection...)")

		val serverSocket = ServerSocket(port)
		val clientSocket = try {
			serverSocket.accept()
		} catch (e: Exception) {
			serverSocket.close()
			throw e
		}

		System.err.println("Client connected from ${clientSocket.remoteSocketAddress}")

		val launcher = LSPLauncher.createServerLauncher(
			server,
			clientSocket.getInputStream(),
			clientSocket.getOutputStream(),
			validate,
			traceWriter
		)

		server.connect(launcher.remoteProxy)
		val listeningFuture = launcher.startListening()

		System.err.println("LSP TCP transport initialized successfully.")
		return LspSession(serverSocket, clientSocket, listeningFuture, exitFuture)
	}

	private fun setupStdioTransport(
		server: ZigocracyLanguageServer,
		exitFuture: CompletableFuture<Boolean>,
		traceWriter: PrintWriter?
	): LspSession {
		System.err.println("Starting LSP server via standard I/O (stdio)...")

		val launcher = LSPLauncher.createServerLauncher(
			server,
			System.`in`,
			System.out,
			validate,
			traceWriter
		)

		server.connect(launcher.remoteProxy)
		val listeningFuture = launcher.startListening()

		System.err.println("LSP stdio transport initialized successfully.")
		return LspSession(null, null, listeningFuture, exitFuture)
	}
}