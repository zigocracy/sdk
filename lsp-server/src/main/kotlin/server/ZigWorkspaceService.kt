package com.zigocracy.sdk.lsp.server

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.WorkspaceService

internal class ZigWorkspaceService(
	private val server: ZigocracyLanguageServer
) : WorkspaceService {
	private var client: LanguageClient? = null

	fun connect(client: LanguageClient) {
		this.client = client
	}

	override fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams) {
		if (server.isServerShutdown()) return

		for (event in params.changes) {
			val uri = event.uri ?: continue
			val path = uriToPath(uri)

			when (event.type) {
				FileChangeType.Created -> {
					server.workspaceContext.vfs.refresh(path, originalPath = uri)
					server.textDocumentService.onFileCreatedOrChanged(uri, path)
				}
				FileChangeType.Changed -> {
					server.workspaceContext.vfs.refresh(path, originalPath = uri)
					server.textDocumentService.onFileCreatedOrChanged(uri, path)
				}
				FileChangeType.Deleted -> {
					server.workspaceContext.vfs.delete(path)
					server.textDocumentService.onFileDeleted(uri, path)
				}
				null -> {}
			}
		}
	}

	override fun didChangeWorkspaceFolders(params: DidChangeWorkspaceFoldersParams) {
		if (server.isServerShutdown()) return

		val added = params.event?.added ?: return
		if (server.workspaceContext.rootPath == null && added.isNotEmpty()) {
			val newRoot = uriToPath(added.first().uri)
			server.updateWorkspaceRoot(newRoot)
		}
	}

	override fun didChangeConfiguration(params: DidChangeConfigurationParams) {}
}