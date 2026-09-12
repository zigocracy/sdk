package com.zigocracy.sdk.lsp.server

import com.zigocracy.sdk.engine.ParsedFile
import com.zigocracy.sdk.engine.vfs.VfsResult
import com.zigocracy.sdk.lsp.analysis.LspDiagnosticCollector
import com.zigocracy.sdk.lsp.analysis.LspTokenCollector
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.TextDocumentService
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

internal class ZigTextDocumentService(
	private val server: ZigocracyLanguageServer
) : TextDocumentService {
	private var client: LanguageClient? = null
	private val parsedFiles = ConcurrentHashMap<String, ParsedFile>()
	private val diagnosticTasks = ConcurrentHashMap<String, CompletableFuture<*>>()

	fun connect(client: LanguageClient) {
		this.client = client
	}

	override fun didOpen(params: DidOpenTextDocumentParams) {
		if (server.isServerShutdown()) return

		val uri = params.textDocument.uri
		val text = params.textDocument.text
		val path = uriToPath(uri)

		val sourceFile = server.workspaceContext.vfs.setOverlay(path, text, originalPath = uri)
		val parsedFile = server.workspaceContext.parse(sourceFile)
		parsedFiles[uri] = parsedFile

		triggerDiagnostics(uri, parsedFile)
	}

	override fun didChange(params: DidChangeTextDocumentParams) {
		if (server.isServerShutdown()) return

		val uri = params.textDocument.uri
		val change = params.contentChanges.firstOrNull() ?: return
		val path = uriToPath(uri)

		val sourceFile = server.workspaceContext.vfs.setOverlay(path, change.text, originalPath = uri)
		val parsedFile = server.workspaceContext.parse(sourceFile)
		parsedFiles[uri] = parsedFile

		triggerDiagnostics(uri, parsedFile)
	}

	override fun didClose(params: DidCloseTextDocumentParams) {
		if (server.isServerShutdown()) return

		val uri = params.textDocument.uri
		val path = uriToPath(uri)
		server.workspaceContext.vfs.removeOverlay(path)

		parsedFiles.remove(uri)
		diagnosticTasks.remove(uri)?.cancel(true)
	}

	override fun didSave(params: DidSaveTextDocumentParams) {
		if (server.isServerShutdown()) return

		val uri = params.textDocument.uri
		val path = uriToPath(uri)
		server.workspaceContext.vfs.refresh(path, originalPath = uri)

		parsedFiles[uri]?.let { triggerDiagnostics(uri, it) }
	}

	fun onFileCreatedOrChanged(uri: String, path: Path) {
		if (server.isServerShutdown()) return

		if (parsedFiles.containsKey(uri)) {
			if (!server.workspaceContext.vfs.hasOverlay(path)) {
				val result = server.workspaceContext.vfs.acquire(path, originalPath = uri)
				if (result is VfsResult.Success) {
					val parsedFile = server.workspaceContext.parse(result.file)
					parsedFiles[uri] = parsedFile
					triggerDiagnostics(uri, parsedFile)
				}
			}
		}
	}

	fun onFileDeleted(uri: String, path: Path) {
		if (server.isServerShutdown()) return

		diagnosticTasks.remove(uri)?.cancel(true)
		val removed = parsedFiles.remove(uri)
		if (removed != null) {
			client?.publishDiagnostics(PublishDiagnosticsParams(uri, emptyList()))
		}
	}

	private fun triggerDiagnostics(uri: String, parsedFile: ParsedFile) {
		diagnosticTasks.remove(uri)?.cancel(true)

		val task = CompletableFuture.supplyAsync {
			computeDiagnosticsOrFallback(uri, parsedFile)
		}.thenAccept { lspDiagnostics ->
			if (parsedFiles[uri] === parsedFile) {
				client?.publishDiagnostics(PublishDiagnosticsParams(uri, lspDiagnostics))
			}
		}

		diagnosticTasks[uri] = task
	}

	override fun semanticTokensFull(params: SemanticTokensParams): CompletableFuture<SemanticTokens> {
		if (server.isServerShutdown()) {
			return rejectIfShutdown()
		}

		val parsedFile = parsedFiles[params.textDocument.uri]
			?: return CompletableFuture.completedFuture(SemanticTokens(emptyList()))

		return CompletableFuture.supplyAsync({
			val tokensData = LspTokenCollector(parsedFile).collectAndEncode()
			SemanticTokens(tokensData)
		})
	}

	fun shutdown() {
		parsedFiles.clear()

		val iterator = diagnosticTasks.values.iterator()
		while (iterator.hasNext()) {
			val task = iterator.next()
			task.cancel(true)
			iterator.remove()
		}
	}

	private fun <T> rejectIfShutdown(): CompletableFuture<T> {
		val failedFuture = CompletableFuture<T>()
		val error = ResponseError(
			ResponseErrorCode.InvalidRequest,
			"Request Rejected: Language server is shutting down.",
			null
		)
		failedFuture.completeExceptionally(ResponseErrorException(error))
		return failedFuture
	}

	private fun computeDiagnosticsOrFallback(uri: String, parsedFile: ParsedFile): List<Diagnostic> {
		return try {
			val collector = LspDiagnosticCollector(
				parsedFile = parsedFile,
				supportsRelatedInformation = server.clientSupportsRelatedInformation,
				workspaceContext = server.workspaceContext,
			)
			collector.collectAndEncode(uri)
		} catch (e: Exception) {
			listOf(createFallbackDiagnostic(e))
		}
	}

	companion object {
		private val zeroPosition = Position(0, 0)
		private val zeroRange = Range(zeroPosition, zeroPosition)
		private const val diagnosticSource = "zigocracy"

		private fun createFallbackDiagnostic(e: Exception): Diagnostic {
			val fallbackDiagnostic = Diagnostic(
				zeroRange,
				"Internal analysis error: ${e.localizedMessage ?: e::class.simpleName}"
			).apply {
				severity = DiagnosticSeverity.Warning
				source = diagnosticSource
			}

			return fallbackDiagnostic
		}
	}
}
