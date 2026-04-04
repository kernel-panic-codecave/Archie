package net.kernelpanicsoft.archie.config.v2.ui.yacl

import net.kernelpanicsoft.archie.config.v2.model.ConfigDocument
import net.kernelpanicsoft.archie.config.v2.runtime.ConfigState
import net.kernelpanicsoft.archie.config.v2.ui.ConfigUiAdapter
import net.kernelpanicsoft.archie.config.v2.ui.ConfigUiAdapterIds
import net.kernelpanicsoft.archie.config.v2.ui.ConfigUiAdapters

/**
 * YACL adapter scaffold.
 *
 * This is intentionally dependency-light for now: no compile-time YACL dependency is required.
 * Registration is gated behind runtime class detection so common can compile on all targets.
 */
object YaclConfigUiAdapter : ConfigUiAdapter {
    override val id: String = ConfigUiAdapterIds.YACL

    private const val YACL_ROOT_CLASS = "dev.isxander.yacl3.api.YetAnotherConfigLib"

    /**
     * Registers the adapter only when YACL appears on the runtime classpath.
     */
    fun registerIfAvailable(): Boolean {
        return if (isYaclPresent()) {
            ConfigUiAdapters.register(this)
            true
        } else {
            false
        }
    }

    fun isYaclPresent(): Boolean {
        return try {
            Class.forName(YACL_ROOT_CLASS)
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    /**
     * Placeholder implementation until full reflective builder wiring lands.
     */
    override fun buildScreen(document: ConfigDocument, state: ConfigState): Any {
        return mapOf(
            "adapter" to id,
            "title" to document.title,
            "categories" to document.categories.size,
            "values" to state.allValues()
        )
    }
}

