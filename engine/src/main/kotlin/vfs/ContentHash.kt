package com.zigocracy.sdk.engine.vfs

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*

/**
 * Deterministic SHA-256 content hash of a source document.
 */
@JvmInline
value class ContentHash private constructor(val hex: String) {
	override fun toString(): String = hex

	companion object {
		/**
		 * Computes the SHA-256 content hash of the provided source [text].
		 *
		 * Time Complexity: Θ(N), where N is the character length of [text].
		 * Memory Complexity: Θ(1) auxiliary.
		 */
		fun compute(text: String): ContentHash {
			val digest = MessageDigest.getInstance("SHA-256")
			val bytes = digest.digest(text.toByteArray(StandardCharsets.UTF_8))
			val hexString = HexFormat.of().formatHex(bytes)
			return ContentHash(hexString)
		}
	}
}