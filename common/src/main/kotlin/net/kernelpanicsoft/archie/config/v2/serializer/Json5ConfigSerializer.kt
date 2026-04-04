package net.kernelpanicsoft.archie.config.v2.serializer

import dev.architectury.platform.Platform
import io.github.xn32.json5k.Json5
import java.nio.file.Path
import net.kernelpanicsoft.archie.config.v2.model.ConfigDocument
import net.kernelpanicsoft.archie.config.v2.runtime.ConfigState

object Json5ConfigV2Serializer : IConfigV2Serializer {
    private val json5 = Json5 {
        prettyPrint = true
        quoteMemberNames = true
        encodeDefaults = true
    }

    override fun configPath(document: ConfigDocument): Path {
        return Platform.getConfigFolder().resolve("${document.id}.v2.json5")
    }

    override fun loadString(document: ConfigDocument, state: ConfigState, raw: String) {
        val persisted = json5.decodeFromString(PersistedConfigV2.serializer(), raw)
        val fields = ConfigV2ValueCodec.collectFields(document)
        persisted.values.forEach { (key, encoded) ->
            val field = fields[key] ?: return@forEach
            val decoded = ConfigV2ValueCodec.decode(field, encoded) ?: return@forEach
            state.setValue(key, decoded)
        }
    }

    override fun saveString(document: ConfigDocument, state: ConfigState): String {
        val fields = ConfigV2ValueCodec.collectFields(document)
        val encodedValues = buildMap<String, String> {
            fields.forEach { (key, field) ->
                val value = state.allValues()[key] ?: return@forEach
                put(key, ConfigV2ValueCodec.encode(field, value))
            }
        }
        return json5.encodeToString(PersistedConfigV2.serializer(), PersistedConfigV2(values = encodedValues))
    }
}

