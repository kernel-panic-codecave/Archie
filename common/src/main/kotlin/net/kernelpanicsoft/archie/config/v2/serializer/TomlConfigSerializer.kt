package net.kernelpanicsoft.archie.config.v2.serializer

import dev.architectury.platform.Platform
import java.nio.file.Path
import net.kernelpanicsoft.archie.config.v2.model.ConfigDocument
import net.kernelpanicsoft.archie.config.v2.runtime.ConfigState
import net.peanuuutz.tomlkt.Toml
import net.peanuuutz.tomlkt.TomlIndentation

object TomlConfigV2Serializer : IConfigV2Serializer {
    private val toml = Toml {
        ignoreUnknownKeys = true
        indentation = TomlIndentation.Tab
    }

    override fun configPath(document: ConfigDocument): Path {
        return Platform.getConfigFolder().resolve("${document.id}.v2.toml")
    }

    override fun loadString(document: ConfigDocument, state: ConfigState, raw: String) {
        val persisted = toml.decodeFromString(PersistedConfigV2.serializer(), raw)
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
        return toml.encodeToString(PersistedConfigV2.serializer(), PersistedConfigV2(values = encodedValues))
    }
}

