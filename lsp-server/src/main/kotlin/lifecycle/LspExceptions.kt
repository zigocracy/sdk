package com.zigocracy.sdk.lsp.lifecycle

class LspConnectionLostException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class LspProtocolException(message: String) : RuntimeException(message)