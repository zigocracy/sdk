package com.zigocracy.sdk.lsp.server

import com.zigocracy.sdk.zig.syntax.VisualGroup
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode
import org.eclipse.lsp4j.services.*
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

internal class ZigocracyLanguageServer(
	private val onExit: (isNormalShutdown: Boolean) -> Unit
) : LanguageServer, LanguageClientAware {
	private lateinit var client: LanguageClient
	private val textDocumentService = ZigTextDocumentService(this)
	private val isShutdownInitiated = AtomicBoolean(false)

	@Volatile
	var clientSupportsRelatedInformation: Boolean = false
		private set

	fun isServerShutdown(): Boolean = isShutdownInitiated.get()

	companion object {
		val TOKEN_TYPES = listOf(
			SemanticTokenTypes.Function,
			SemanticTokenTypes.Keyword,
			SemanticTokenTypes.String,
			SemanticTokenTypes.Number,
			SemanticTokenTypes.Comment,
			SemanticTokenTypes.Variable,
			SemanticTokenTypes.Operator
		)
		val TOKEN_MODIFIERS = listOf(
			SemanticTokenModifiers.DefaultLibrary,
			SemanticTokenModifiers.Documentation
		)

		val TOKEN_TYPE_INDICES: Map<String, Int> =
			TOKEN_TYPES.withIndex().associate { it.value to it.index }.withDefault { -1 }

		private fun bitmask(vararg modifiers: String): Int {
			var mask = 0
			for (modifier in modifiers) {
				val idx = TOKEN_MODIFIERS.indexOf(modifier)
				if (idx >= 0) {
					mask = mask or (1 shl idx)
				}
			}
			return mask
		}

		val VISUAL_GROUP_MODIFIERS: Map<VisualGroup, Int> = EnumMap<VisualGroup, Int>(VisualGroup::class.java).apply {
			put(VisualGroup.BuiltinIdentifier, bitmask(SemanticTokenModifiers.DefaultLibrary))
			put(VisualGroup.DocComment, bitmask(SemanticTokenModifiers.Documentation))
		}

		val VISUAL_GROUP_TYPE_INDICES: Map<VisualGroup, Int> = EnumMap<VisualGroup, Int>(VisualGroup::class.java).apply {
			put(VisualGroup.BuiltinIdentifier, TOKEN_TYPE_INDICES.getValue(SemanticTokenTypes.Function))
			put(VisualGroup.Keyword, TOKEN_TYPE_INDICES.getValue(SemanticTokenTypes.Keyword))
			put(VisualGroup.String, TOKEN_TYPE_INDICES.getValue(SemanticTokenTypes.String))
			put(VisualGroup.Number, TOKEN_TYPE_INDICES.getValue(SemanticTokenTypes.Number))
			put(VisualGroup.Comment, TOKEN_TYPE_INDICES.getValue(SemanticTokenTypes.Comment))
			put(VisualGroup.DocComment, TOKEN_TYPE_INDICES.getValue(SemanticTokenTypes.Comment))
			put(VisualGroup.Identifier, TOKEN_TYPE_INDICES.getValue(SemanticTokenTypes.Variable))
			put(VisualGroup.Operator, TOKEN_TYPE_INDICES.getValue(SemanticTokenTypes.Operator))
		}.withDefault { -1 }
	}

	override fun initialize(params: InitializeParams): CompletableFuture<InitializeResult> {
		clientSupportsRelatedInformation = params.capabilities
			.textDocument
			?.publishDiagnostics
			?.relatedInformation ?: false

		val result = InitializeResult()
		val caps = ServerCapabilities().apply {
			setTextDocumentSync(TextDocumentSyncKind.Full)

			semanticTokensProvider = SemanticTokensWithRegistrationOptions().apply {
				legend = SemanticTokensLegend(TOKEN_TYPES, TOKEN_MODIFIERS)
				setFull(true)
			}
		}

		result.capabilities = caps
		return CompletableFuture.completedFuture(result)
	}

	override fun connect(client: LanguageClient) {
		this.client = client
		this.textDocumentService.connect(client)
	}

	override fun getTextDocumentService(): TextDocumentService = textDocumentService

	override fun getWorkspaceService(): WorkspaceService = object : WorkspaceService {
		override fun didChangeConfiguration(params: DidChangeConfigurationParams) {}
		override fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams) {}
	}

	override fun shutdown(): CompletableFuture<Any> {
		val alreadyShutdown = isShutdownInitiated.getAndSet(true)

		if (alreadyShutdown) {
			val failedFuture = CompletableFuture<Any>()
			val error = ResponseError(
				ResponseErrorCode.InvalidRequest,
				"Request Ignored: A server shutdown request is already in progress",
				null
			)
			failedFuture.completeExceptionally(ResponseErrorException(error))
			return failedFuture
		}

		textDocumentService.shutdown()
		return CompletableFuture.completedFuture(null)
	}

	override fun exit() {
		val isNormalShutdown = isServerShutdown()

		onExit(isNormalShutdown)
	}
}