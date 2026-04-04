package net.kernelpanicsoft.archie.config.v2.serializer

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import net.kernelpanicsoft.archie.config.CommonKeyCode
import net.kernelpanicsoft.archie.config.v2.model.BooleanField
import net.kernelpanicsoft.archie.config.v2.model.ChoiceField
import net.kernelpanicsoft.archie.config.v2.model.ChoiceListField
import net.kernelpanicsoft.archie.config.v2.model.ChoiceMapField
import net.kernelpanicsoft.archie.config.v2.model.ColorField
import net.kernelpanicsoft.archie.config.v2.model.ColorListField
import net.kernelpanicsoft.archie.config.v2.model.ConfigCategory
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
import net.kernelpanicsoft.archie.gui.util.KColor

@Serializable
internal data class PersistedConfigV2(
    val version: Int = 1,
    val values: Map<String, String> = mapOf()
)

internal object ConfigV2ValueCodec {
    private val json = Json {
        prettyPrint = false
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun collectFields(document: ConfigDocument): Map<String, ConfigField<*>> {
        val out = linkedMapOf<String, ConfigField<*>>()
        collectCategories(document.categories, out)
        return out
    }

    private fun collectCategories(categories: List<ConfigCategory>, out: MutableMap<String, ConfigField<*>>) {
        categories.forEach { category ->
            category.fields.forEach { field -> out[field.key] = field }
            collectCategories(category.children, out)
        }
    }

    fun encode(field: ConfigField<*>, value: Any): String {
        @Suppress("UNCHECKED_CAST")
        return when (field) {
            is BooleanField -> json.encodeToString(Boolean.serializer(), value as Boolean)
            is IntField -> json.encodeToString(Int.serializer(), value as Int)
            is LongField -> json.encodeToString(Long.serializer(), value as Long)
            is FloatField -> json.encodeToString(Float.serializer(), value as Float)
            is DoubleField -> json.encodeToString(Double.serializer(), value as Double)
            is IntSliderField -> json.encodeToString(Int.serializer(), value as Int)
            is LongSliderField -> json.encodeToString(Long.serializer(), value as Long)
            is StringField -> json.encodeToString(String.serializer(), value as String)
            is RegistryField -> json.encodeToString(String.serializer(), value as String)
            is KeyCodeField -> json.encodeToString(CommonKeyCode.serializer(), value as CommonKeyCode)
            is SpecField -> json.encodeToString(MapSerializer(String.serializer(), String.serializer()), value as Map<String, String>)
            is ChoiceField -> json.encodeToString(String.serializer(), value as String)
            is ChoiceListField -> json.encodeToString(ListSerializer(String.serializer()), value as List<String>)
            is ChoiceMapField -> json.encodeToString(MapSerializer(String.serializer(), String.serializer()), value as Map<String, String>)
            is ColorField -> json.encodeToString(KColor.serializer(), value as KColor)
            is IntListField -> json.encodeToString(ListSerializer(Int.serializer()), value as List<Int>)
            is LongListField -> json.encodeToString(ListSerializer(Long.serializer()), value as List<Long>)
            is FloatListField -> json.encodeToString(ListSerializer(Float.serializer()), value as List<Float>)
            is DoubleListField -> json.encodeToString(ListSerializer(Double.serializer()), value as List<Double>)
            is StringListField -> json.encodeToString(ListSerializer(String.serializer()), value as List<String>)
            is RegistryListField -> json.encodeToString(ListSerializer(String.serializer()), value as List<String>)
            is KeyCodeListField -> json.encodeToString(ListSerializer(CommonKeyCode.serializer()), value as List<CommonKeyCode>)
            is SpecListField -> json.encodeToString(
                ListSerializer(MapSerializer(String.serializer(), String.serializer())),
                value as List<Map<String, String>>
            )
            is ColorListField -> json.encodeToString(ListSerializer(KColor.serializer()), value as List<KColor>)
            is IntMapField -> json.encodeToString(MapSerializer(String.serializer(), Int.serializer()), value as Map<String, Int>)
            is LongMapField -> json.encodeToString(MapSerializer(String.serializer(), Long.serializer()), value as Map<String, Long>)
            is FloatMapField -> json.encodeToString(MapSerializer(String.serializer(), Float.serializer()), value as Map<String, Float>)
            is DoubleMapField -> json.encodeToString(MapSerializer(String.serializer(), Double.serializer()), value as Map<String, Double>)
            is StringMapField -> json.encodeToString(MapSerializer(String.serializer(), String.serializer()), value as Map<String, String>)
            is RegistryMapField -> json.encodeToString(MapSerializer(String.serializer(), String.serializer()), value as Map<String, String>)
            is KeyCodeMapField -> json.encodeToString(
                MapSerializer(String.serializer(), CommonKeyCode.serializer()),
                value as Map<String, CommonKeyCode>
            )
            is SpecMapField -> json.encodeToString(
                MapSerializer(String.serializer(), MapSerializer(String.serializer(), String.serializer())),
                value as Map<String, Map<String, String>>
            )
        }
    }

    fun decode(field: ConfigField<*>, encoded: String): Any? {
        return runCatching {
            when (field) {
                is BooleanField -> json.decodeFromString(Boolean.serializer(), encoded)
                is IntField -> json.decodeFromString(Int.serializer(), encoded)
                is LongField -> json.decodeFromString(Long.serializer(), encoded)
                is FloatField -> json.decodeFromString(Float.serializer(), encoded)
                is DoubleField -> json.decodeFromString(Double.serializer(), encoded)
                is IntSliderField -> json.decodeFromString(Int.serializer(), encoded)
                is LongSliderField -> json.decodeFromString(Long.serializer(), encoded)
                is StringField -> json.decodeFromString(String.serializer(), encoded)
                is RegistryField -> json.decodeFromString(String.serializer(), encoded)
                is KeyCodeField -> json.decodeFromString(CommonKeyCode.serializer(), encoded)
                is SpecField -> json.decodeFromString(MapSerializer(String.serializer(), String.serializer()), encoded)
                is ChoiceField -> json.decodeFromString(String.serializer(), encoded)
                is ChoiceListField -> json.decodeFromString(ListSerializer(String.serializer()), encoded)
                is ChoiceMapField -> json.decodeFromString(MapSerializer(String.serializer(), String.serializer()), encoded)
                is ColorField -> json.decodeFromString(KColor.serializer(), encoded)
                is IntListField -> json.decodeFromString(ListSerializer(Int.serializer()), encoded)
                is LongListField -> json.decodeFromString(ListSerializer(Long.serializer()), encoded)
                is FloatListField -> json.decodeFromString(ListSerializer(Float.serializer()), encoded)
                is DoubleListField -> json.decodeFromString(ListSerializer(Double.serializer()), encoded)
                is StringListField -> json.decodeFromString(ListSerializer(String.serializer()), encoded)
                is RegistryListField -> json.decodeFromString(ListSerializer(String.serializer()), encoded)
                is KeyCodeListField -> json.decodeFromString(ListSerializer(CommonKeyCode.serializer()), encoded)
                is SpecListField -> json.decodeFromString(
                    ListSerializer(MapSerializer(String.serializer(), String.serializer())),
                    encoded
                )
                is ColorListField -> json.decodeFromString(ListSerializer(KColor.serializer()), encoded)
                is IntMapField -> json.decodeFromString(MapSerializer(String.serializer(), Int.serializer()), encoded)
                is LongMapField -> json.decodeFromString(MapSerializer(String.serializer(), Long.serializer()), encoded)
                is FloatMapField -> json.decodeFromString(MapSerializer(String.serializer(), Float.serializer()), encoded)
                is DoubleMapField -> json.decodeFromString(MapSerializer(String.serializer(), Double.serializer()), encoded)
                is StringMapField -> json.decodeFromString(MapSerializer(String.serializer(), String.serializer()), encoded)
                is RegistryMapField -> json.decodeFromString(MapSerializer(String.serializer(), String.serializer()), encoded)
                is KeyCodeMapField -> json.decodeFromString(
                    MapSerializer(String.serializer(), CommonKeyCode.serializer()),
                    encoded
                )
                is SpecMapField -> json.decodeFromString(
                    MapSerializer(String.serializer(), MapSerializer(String.serializer(), String.serializer())),
                    encoded
                )
            }
        }.getOrNull()
    }
}


