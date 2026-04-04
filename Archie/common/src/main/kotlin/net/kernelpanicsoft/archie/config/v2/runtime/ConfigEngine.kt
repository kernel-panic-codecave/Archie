package net.kernelpanicsoft.archie.config.v2.runtime

import dev.architectury.platform.Mod
import net.kernelpanicsoft.archie.APlatform
import net.kernelpanicsoft.archie.config.AConfigPlatform
import net.kernelpanicsoft.archie.config.v2.builder.ConfigDocObject
import net.kernelpanicsoft.archie.config.v2.model.ConfigDocument
import net.kernelpanicsoft.archie.config.v2.serializer.IConfigSerializer
import net.kernelpanicsoft.archie.config.v2.serializer.Json5ConfigSerializer
import net.kernelpanicsoft.archie.config.v2.serializer.TomlConfigSerializer
import net.kernelpanicsoft.archie.config.v2.ui.ConfigUiAdapterIds
import net.kernelpanicsoft.archie.config.v2.ui.ConfigUiAdapters
import net.minecraft.client.gui.screens.Screen

/**
 * Runtime helper that binds a [ConfigDocument] to state and optional UI adapters.
 */
class ConfigEngine(val document: ConfigDocument) {
    val state: ConfigState = ConfigState(document)

    val serializer: IConfigSerializer = when (APlatform.platform) {
        "fabric" -> Json5ConfigSerializer
        "neoforge" -> TomlConfigSerializer
        else -> throw UnsupportedOperationException("Unsupported platform: ${APlatform.platform}")
    }

    fun validationErrors(): List<String> = state.validate()

    fun load() {
        serializer.load(document, state)
    }

    fun save() {
        serializer.save(document, state)
    }

    companion object {
        fun fromDsl(dsl: ConfigDocObject, load: Boolean = true): ConfigEngine {
            val engine = ConfigEngine(dsl.document)
            dsl.bind(engine.state)
            if (load) {
                engine.load()
            }
            return engine
        }
    }

    fun registerScreenHandler(mod: Mod)
    {
        AConfigPlatform.registerScreenHandler(mod) {
            buildPreferredScreen(preferred = ConfigUiAdapterIds.YACL) ?: { it }
        }
    }

    /**
     * Builds a UI screen with a registered adapter. Returns null when the adapter is missing.
     */
    fun buildScreen(adapterId: String): ((Screen) -> Screen)? {
        val adapter = ConfigUiAdapters.byId(adapterId) ?: return null
        return adapter.buildScreen(document, state)
    }

    /**
     * Builds a UI screen by preference order.
     *
     * Default policy prefers YACL then Cloth when both are registered.
     */
    fun buildPreferredScreen(
        preferred: String? = null,
        fallbackOrder: List<String> = listOf(ConfigUiAdapterIds.YACL, ConfigUiAdapterIds.CLOTH)
    ): ((Screen) -> Screen)? {
        val order = buildList {
            if (preferred != null) add(preferred)
            addAll(fallbackOrder)
        }.distinct()

        val adapter = ConfigUiAdapters.preferred(*order.toTypedArray()) ?: return null
        return adapter.buildScreen(document, state)
    }
}


