package com.zigocracy.sdk.lsp.lifecycle

import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

class LspSession(
	private val serverSocket: ServerSocket?,
	private val clientSocket: Socket?,
	private val listeningFuture: Future<Void>,
	private val exitFuture: CompletableFuture<Boolean>
) {
	fun close(isNormalShutdown: Boolean) {
		if (!listeningFuture.isCancelled) {
			listeningFuture.cancel(true)
		}
		runCatching { clientSocket?.close() }
		runCatching { serverSocket?.close() }
		exitFuture.complete(isNormalShutdown)
	}

	fun serve() {
		try {
			val isNormalShutdown = exitFuture.get()

			if (!isNormalShutdown) {
				throw LspProtocolException("LSP server exit notification was sent without a prior shutdown request.")
			}
		} catch (e: ExecutionException) {
			throw LspConnectionLostException("LSP connection was abruptly lost or closed by the remote client.", e.cause)
		} catch (e: LspProtocolException) {
			throw e
		} catch (e: Exception) {
			throw RuntimeException("LSP server session encountered an unhandled internal crash.", e)
		} finally {
			runCatching { clientSocket?.close() }
			runCatching { serverSocket?.close() }
		}
	}
}