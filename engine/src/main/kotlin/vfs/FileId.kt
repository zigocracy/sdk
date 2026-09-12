package com.zigocracy.sdk.engine.vfs

/**
 * Type-safe 32-bit identifier for an interned file within a [Vfs] instance.
 */
@JvmInline
value class FileId internal constructor(val raw: Int)