package net.kernelpanicsoft.archie.config.v2.serializer

import dev.architectury.platform.Platform
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import java.nio.file.Path
import net.kernelpanicsoft.archie.config.v2.model.ConfigDocument
import net.kernelpanicsoft.archie.config.v2.runtime.ConfigState

object Json5ConfigSerializer : IConfigSerializer {
    @Serializable
    private data class Json5PersistedConfig(
        val version: Int = 1,
        val values: Map<String, JsonElement> = mapOf(),
    )

    private val json5 = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override fun configPath(document: ConfigDocument): Path {
        return Platform.getConfigFolder().resolve("${document.id}.json5")
    }

    override fun loadString(document: ConfigDocument, state: ConfigState, raw: String) {
        val persisted = json5.decodeFromString(Json5PersistedConfig.serializer(), raw)
        val fields = ConfigValueCodec.collectFields(document)
        persisted.values.forEach { (key, encoded) ->
            val field = fields[key] ?: return@forEach
            val decoded = ConfigValueCodec.decodeJsonElement(field, encoded)
                ?: decodeLegacyString(field, encoded)
                ?: return@forEach
            state.setValue(key, decoded)
        }
    }

    override fun saveString(document: ConfigDocument, state: ConfigState): String {
        val fields = ConfigValueCodec.collectFields(document)
        val encodedValues = buildMap<String, JsonElement> {
            fields.forEach { (key, field) ->
                val value = state.allValues()[key] ?: return@forEach
                put(key, ConfigValueCodec.encodeJsonElement(field, value))
            }
        }
        return json5.encodeToString(Json5PersistedConfig.serializer(), Json5PersistedConfig(values = encodedValues))
    }

    private fun decodeLegacyString(field: net.kernelpanicsoft.archie.config.v2.model.ConfigField<*>, encoded: JsonElement): Any? {
        val primitive = encoded as? JsonPrimitive ?: return null
        if (!primitive.isString) return null
        return ConfigValueCodec.decode(field, primitive.content)
    }
}

