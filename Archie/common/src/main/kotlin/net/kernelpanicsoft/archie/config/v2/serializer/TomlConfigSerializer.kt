package net.kernelpanicsoft.archie.config.v2.serializer

import dev.architectury.platform.Platform
import net.kernelpanicsoft.archie.config.CommonKeyCode
import net.kernelpanicsoft.archie.config.v2.model.BooleanField
import net.kernelpanicsoft.archie.config.v2.model.ChoiceField
import net.kernelpanicsoft.archie.config.v2.model.ChoiceListField
import net.kernelpanicsoft.archie.config.v2.model.ChoiceMapField
import net.kernelpanicsoft.archie.config.v2.model.ColorField
import net.kernelpanicsoft.archie.config.v2.model.ColorListField
import net.kernelpanicsoft.archie.config.v2.model.ConfigDocument
import net.kernelpanicsoft.archie.config.v2.model.ConfigField
import net.kernelpanicsoft.archie.config.v2.model.DoubleField
import net.kernelpanicsoft.archie.config.v2.model.DoubleListField
import net.kernelpanicsoft.archie.config.v2.model.DoubleMapField
import net.kernelpanicsoft.archie.config.v2.model.FloatField
import net.kernelpanicsoft.archie.config.v2.model.FloatListField
import net.kernelpanicsoft.archie.config.v2.model.FloatMapField
import net.kernelpanicsoft.archie.config.v2.model.IntField
import net.kernelpanicsoft.archie.config.v2.model.IntListField
import net.kernelpanicsoft.archie.config.v2.model.IntMapField
import net.kernelpanicsoft.archie.config.v2.model.IntSliderField
import net.kernelpanicsoft.archie.config.v2.model.KeyCodeField
import net.kernelpanicsoft.archie.config.v2.model.KeyCodeListField
import net.kernelpanicsoft.archie.config.v2.model.KeyCodeMapField
import net.kernelpanicsoft.archie.config.v2.model.LongField
import net.kernelpanicsoft.archie.config.v2.model.LongListField
import net.kernelpanicsoft.archie.config.v2.model.LongMapField
import net.kernelpanicsoft.archie.config.v2.model.LongSliderField
import net.kernelpanicsoft.archie.config.v2.model.RegistryField
import net.kernelpanicsoft.archie.config.v2.model.RegistryListField
import net.kernelpanicsoft.archie.config.v2.model.RegistryMapField
import net.kernelpanicsoft.archie.config.v2.model.SpecField
import net.kernelpanicsoft.archie.config.v2.model.SpecListField
import net.kernelpanicsoft.archie.config.v2.model.SpecMapField
import net.kernelpanicsoft.archie.config.v2.model.StringField
import net.kernelpanicsoft.archie.config.v2.model.StringListField
import net.kernelpanicsoft.archie.config.v2.model.StringMapField
import kotlinx.serialization.Serializable
import net.kernelpanicsoft.archie.config.v2.runtime.ConfigState
import net.kernelpanicsoft.archie.gui.util.KColor
import net.peanuuutz.tomlkt.Toml
import net.peanuuutz.tomlkt.TomlIndentation
import java.nio.file.Path

object TomlConfigSerializer : IConfigSerializer {
    @Serializable
    private data class TomlPersistedConfig(
        val version: Int = 1,
        // Legacy storage format (stringified JSON payloads).
        val values: Map<String, String> = mapOf(),

        // Typed TOML buckets.
        val booleans: Map<String, Boolean> = mapOf(),
        val ints: Map<String, Int> = mapOf(),
        val longs: Map<String, Long> = mapOf(),
        val floats: Map<String, Float> = mapOf(),
        val doubles: Map<String, Double> = mapOf(),
        val strings: Map<String, String> = mapOf(),
        val keycodes: Map<String, CommonKeyCode> = mapOf(),
        val colors: Map<String, KColor> = mapOf(),

        val intLists: Map<String, List<Int>> = mapOf(),
        val longLists: Map<String, List<Long>> = mapOf(),
        val floatLists: Map<String, List<Float>> = mapOf(),
        val doubleLists: Map<String, List<Double>> = mapOf(),
        val stringLists: Map<String, List<String>> = mapOf(),
        val keycodeLists: Map<String, List<CommonKeyCode>> = mapOf(),
        val colorLists: Map<String, List<KColor>> = mapOf(),
        val specLists: Map<String, List<Map<String, String>>> = mapOf(),

        val intMaps: Map<String, Map<String, Int>> = mapOf(),
        val longMaps: Map<String, Map<String, Long>> = mapOf(),
        val floatMaps: Map<String, Map<String, Float>> = mapOf(),
        val doubleMaps: Map<String, Map<String, Double>> = mapOf(),
        val stringMaps: Map<String, Map<String, String>> = mapOf(),
        val keycodeMaps: Map<String, Map<String, CommonKeyCode>> = mapOf(),
        val colorMaps: Map<String, Map<String, KColor>> = mapOf(),
        val specMaps: Map<String, Map<String, Map<String, String>>> = mapOf(),
    )

    private val toml = Toml {
        ignoreUnknownKeys = true
        indentation = TomlIndentation.Tab
    }

    override fun configPath(document: ConfigDocument): Path {
        return Platform.getConfigFolder().resolve("${document.id}.toml")
    }

    override fun loadString(document: ConfigDocument, state: ConfigState, raw: String) {
        val persisted = toml.decodeFromString(TomlPersistedConfig.serializer(), raw)
        val fields = ConfigValueCodec.collectFields(document)

        fun <T> applyTyped(values: Map<String, T>) {
            values.forEach { (key, value) ->
                if (fields.containsKey(key)) {
                    state.setValue(key, value as Any)
                }
            }
        }

        applyTyped(persisted.booleans)
        applyTyped(persisted.ints)
        applyTyped(persisted.longs)
        applyTyped(persisted.floats)
        applyTyped(persisted.doubles)
        applyTyped(persisted.strings)
        applyTyped(persisted.keycodes)
        applyTyped(persisted.colors)

        applyTyped(persisted.intLists)
        applyTyped(persisted.longLists)
        applyTyped(persisted.floatLists)
        applyTyped(persisted.doubleLists)
        applyTyped(persisted.stringLists)
        applyTyped(persisted.keycodeLists)
        applyTyped(persisted.colorLists)
        applyTyped(persisted.specLists)

        applyTyped(persisted.intMaps)
        applyTyped(persisted.longMaps)
        applyTyped(persisted.floatMaps)
        applyTyped(persisted.doubleMaps)
        applyTyped(persisted.stringMaps)
        applyTyped(persisted.keycodeMaps)
        applyTyped(persisted.colorMaps)
        applyTyped(persisted.specMaps)

        // Legacy fallback for existing stringified TOML configs.
        persisted.values.forEach { (key, encoded) ->
            val field = fields[key] ?: return@forEach
            val decoded = ConfigValueCodec.decode(field, encoded) ?: return@forEach
            state.setValue(key, decoded)
        }
    }

    override fun saveString(document: ConfigDocument, state: ConfigState): String {
        val fields = ConfigValueCodec.collectFields(document)
        val booleans = mutableMapOf<String, Boolean>()
        val ints = mutableMapOf<String, Int>()
        val longs = mutableMapOf<String, Long>()
        val floats = mutableMapOf<String, Float>()
        val doubles = mutableMapOf<String, Double>()
        val strings = mutableMapOf<String, String>()
        val keycodes = mutableMapOf<String, CommonKeyCode>()
        val colors = mutableMapOf<String, KColor>()

        val intLists = mutableMapOf<String, List<Int>>()
        val longLists = mutableMapOf<String, List<Long>>()
        val floatLists = mutableMapOf<String, List<Float>>()
        val doubleLists = mutableMapOf<String, List<Double>>()
        val stringLists = mutableMapOf<String, List<String>>()
        val keycodeLists = mutableMapOf<String, List<CommonKeyCode>>()
        val colorLists = mutableMapOf<String, List<KColor>>()
        val specLists = mutableMapOf<String, List<Map<String, String>>>()

        val intMaps = mutableMapOf<String, Map<String, Int>>()
        val longMaps = mutableMapOf<String, Map<String, Long>>()
        val floatMaps = mutableMapOf<String, Map<String, Float>>()
        val doubleMaps = mutableMapOf<String, Map<String, Double>>()
        val stringMaps = mutableMapOf<String, Map<String, String>>()
        val keycodeMaps = mutableMapOf<String, Map<String, CommonKeyCode>>()
        val colorMaps = mutableMapOf<String, Map<String, KColor>>()
        val specMaps = mutableMapOf<String, Map<String, Map<String, String>>>()

        @Suppress("UNCHECKED_CAST")
        fun assign(field: ConfigField<*>, key: String, value: Any) {
            when (field) {
                is BooleanField -> booleans[key] = value as Boolean
                is IntField, is IntSliderField -> ints[key] = value as Int
                is LongField, is LongSliderField -> longs[key] = value as Long
                is FloatField -> floats[key] = value as Float
                is DoubleField -> doubles[key] = value as Double
                is StringField, is RegistryField, is ChoiceField -> strings[key] = value as String
                is KeyCodeField -> keycodes[key] = value as CommonKeyCode
                is ColorField -> colors[key] = value as KColor

                is IntListField -> intLists[key] = value as List<Int>
                is LongListField -> longLists[key] = value as List<Long>
                is FloatListField -> floatLists[key] = value as List<Float>
                is DoubleListField -> doubleLists[key] = value as List<Double>
                is StringListField, is RegistryListField, is ChoiceListField -> stringLists[key] = value as List<String>
                is KeyCodeListField -> keycodeLists[key] = value as List<CommonKeyCode>
                is ColorListField -> colorLists[key] = value as List<KColor>
                is SpecListField -> specLists[key] = value as List<Map<String, String>>

                is IntMapField -> intMaps[key] = value as Map<String, Int>
                is LongMapField -> longMaps[key] = value as Map<String, Long>
                is FloatMapField -> floatMaps[key] = value as Map<String, Float>
                is DoubleMapField -> doubleMaps[key] = value as Map<String, Double>
                is StringMapField, is RegistryMapField, is ChoiceMapField, is SpecField -> {
                    stringMaps[key] = value as Map<String, String>
                }
                is KeyCodeMapField -> keycodeMaps[key] = value as Map<String, CommonKeyCode>
                is SpecMapField -> specMaps[key] = value as Map<String, Map<String, String>>
            }
        }

        state.allValues().forEach { (key, value) ->
            val field = fields[key] ?: return@forEach
            assign(field, key, value)
        }

        return toml.encodeToString(
            TomlPersistedConfig.serializer(),
            TomlPersistedConfig(
                booleans = booleans,
                ints = ints,
                longs = longs,
                floats = floats,
                doubles = doubles,
                strings = strings,
                keycodes = keycodes,
                colors = colors,
                intLists = intLists,
                longLists = longLists,
                floatLists = floatLists,
                doubleLists = doubleLists,
                stringLists = stringLists,
                keycodeLists = keycodeLists,
                colorLists = colorLists,
                specLists = specLists,
                intMaps = intMaps,
                longMaps = longMaps,
                floatMaps = floatMaps,
                doubleMaps = doubleMaps,
                stringMaps = stringMaps,
                keycodeMaps = keycodeMaps,
                colorMaps = colorMaps,
                specMaps = specMaps,
            )
        )
    }
}

