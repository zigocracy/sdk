package com.zigocracy.sdk.lsp.server

import java.net.URI
import java.nio.file.Path

internal fun uriToPath(uri: String): Path {
	return try {
		val parsed = URI.create(uri)
		if (parsed.scheme.equals("file", ignoreCase = true)) {
			Path.of(parsed)
		} else {
			Path.of(uri.removePrefix("file://").removePrefix("file:/"))
		}
	} catch (e: Exception) {
		Path.of(uri)
	}
}
