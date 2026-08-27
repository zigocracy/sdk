package com.zigocracy.sdk.lsp.analysis

import com.zigocracy.sdk.zig.syntax.SyntaxStream
import com.zigocracy.sdk.zig.text.SourceFile

@JvmRecord
internal data class DocumentSnapshot(
	val text: String,
	val source: SourceFile,
	val stream: SyntaxStream,
)