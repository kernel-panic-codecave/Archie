package net.kernelpanicsoft.archie.config.v2.ui

import net.kernelpanicsoft.archie.config.v2.model.ConfigDocument
import net.kernelpanicsoft.archie.config.v2.runtime.ConfigState
import net.minecraft.client.gui.screens.Screen

/**
 * Client-only UI adapter contract. Implementations should live in loader/client code.
 */
interface ConfigUiAdapter {
    val id: String

    fun register() {
        ConfigUiAdapters.register(this)
    }

    /**
     * Returns a loader-specific screen object.
     */
    fun buildScreen(document: ConfigDocument, state: ConfigState): (Screen) -> Screen
}

object ConfigUiAdapterIds {
    const val CLOTH = "cloth"
    const val YACL = "yacl"
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

    /**
     * Picks the first registered adapter that matches preference order.
     */
    fun preferred(vararg ids: String): ConfigUiAdapter? {
        ids.forEach { id ->
            val adapter = adapters[id]
            if (adapter != null) {
                return adapter
            }
        }
        return null
    }

    fun allIds(): Set<String> = adapters.keys
}


