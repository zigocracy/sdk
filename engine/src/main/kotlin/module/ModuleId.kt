package com.zigocracy.sdk.engine.module

/**
 * Unique identifier of a [Module] within the workspace module graph.
 */
@JvmInline
value class ModuleId internal constructor(val value: Int)