package net.kernelpanicsoft.archie.config.v2.ui

import net.kernelpanicsoft.archie.config.v2.model.ConfigDocument
import net.kernelpanicsoft.archie.config.v2.runtime.ConfigState

/**
 * Client-only UI adapter contract. Implementations should live in loader/client code.
 */
interface ConfigUiAdapter {
    val id: String

    /**
     * Returns a loader-specific screen object.
     */
    fun buildScreen(document: ConfigDocument, state: ConfigState): Any
}

/**
 * Simple registry so common code can request an adapter by id.
 */
object ConfigUiAdapters {
    private val adapters = mutableMapOf<String, ConfigUiAdapter>()

    fun register(adapter: ConfigUiAdapter) {
        adapters[adapter.id] = adapter
    }

    fun byId(id: String): ConfigUiAdapter? = adapters[id]

    fun allIds(): Set<String> = adapters.keys
}

