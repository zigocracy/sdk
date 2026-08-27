package com.zigocracy.sdk.lsp.server

import com.zigocracy.sdk.lsp.analysis.DocumentSnapshot
import com.zigocracy.sdk.lsp.analysis.LspDiagnosticCollector
import com.zigocracy.sdk.lsp.analysis.LspTokenCollector
import com.zigocracy.sdk.zig.parser.Parser
import com.zigocracy.sdk.zig.text.SourceFile
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.TextDocumentService
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

internal class ZigTextDocumentService(
	private val server: ZigocracyLanguageServer
) : TextDocumentService {
	private var client: LanguageClient? = null
	private val documentSnapshots = ConcurrentHashMap<String, DocumentSnapshot>()
	private val diagnosticTasks = ConcurrentHashMap<String, CompletableFuture<*>>()

	fun connect(client: LanguageClient) {
		this.client = client
	}

	override fun didOpen(params: DidOpenTextDocumentParams) {
		if (server.isServerShutdown()) return

		val uri = params.textDocument.uri
		val text = params.textDocument.text

		val snapshot = createNewSnapshot(text)
		documentSnapshots[uri] = snapshot

		triggerDiagnostics(uri, snapshot)
	}

	override fun didChange(params: DidChangeTextDocumentParams) {
		if (server.isServerShutdown()) return

		val uri = params.textDocument.uri
		val change = params.contentChanges.firstOrNull() ?: return

		val snapshot = createNewSnapshot(change.text)
		documentSnapshots[uri] = snapshot

		triggerDiagnostics(uri, snapshot)
	}

	override fun didClose(params: DidCloseTextDocumentParams) {
		if (server.isServerShutdown()) return

		val uri = params.textDocument.uri
		documentSnapshots.remove(uri)
		diagnosticTasks.remove(uri)?.cancel(true)
	}

	override fun didSave(params: DidSaveTextDocumentParams) {}

	private fun createNewSnapshot(text: String): DocumentSnapshot {
		val sourceFile = SourceFile.forTesting(text)
		val parserResult = Parser.parseSyntax(sourceFile)

		return DocumentSnapshot(text, parserResult.source, parserResult.stream)
	}

	private fun triggerDiagnostics(uri: String, snapshot: DocumentSnapshot) {
		diagnosticTasks.remove(uri)?.cancel(true)

		val task = CompletableFuture.supplyAsync {
			computeDiagnosticsOrFallback(uri, snapshot)
		}.thenAccept { lspDiagnostics ->
			if (documentSnapshots[uri] === snapshot) {
				client?.publishDiagnostics(PublishDiagnosticsParams(uri, lspDiagnostics))
			}
		}

		diagnosticTasks[uri] = task
	}

	override fun semanticTokensFull(params: SemanticTokensParams): CompletableFuture<SemanticTokens> {
		if (server.isServerShutdown()) {
			return rejectIfShutdown()
		}

		val snapshot = documentSnapshots[params.textDocument.uri]
			?: return CompletableFuture.completedFuture(SemanticTokens(emptyList()))

		return CompletableFuture.supplyAsync({
			val tokensData = LspTokenCollector(snapshot).collectAndEncode()
			SemanticTokens(tokensData)
		})
	}

	fun shutdown() {
		documentSnapshots.clear()

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

	private fun computeDiagnosticsOrFallback(uri: String, snapshot: DocumentSnapshot): List<Diagnostic> {
		return try {
			val collector = LspDiagnosticCollector(snapshot, server.clientSupportsRelatedInformation)
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