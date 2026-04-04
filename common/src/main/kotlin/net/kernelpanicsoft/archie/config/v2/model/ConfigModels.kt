package net.kernelpanicsoft.archie.config.v2.model

import net.kernelpanicsoft.archie.config.CommonKeyCode
import net.kernelpanicsoft.archie.gui.util.KColor

/**
 * A complete, UI-agnostic config document.
 */
data class ConfigDocument(
    val id: String,
    val title: String,
    val categories: List<ConfigCategory>
)

/**
 * A category groups a list of fields and nested categories.
 */
data class ConfigCategory(
    val id: String,
    val title: String,
    val fields: List<ConfigField<*>> = listOf(),
    val children: List<ConfigCategory> = listOf()
)

/**
 * Base contract for all field definitions.
 */
sealed interface ConfigField<T> {
    val key: String
    val title: String
    val description: String?
    val defaultValue: T
}

data class BooleanField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: Boolean = false
) : ConfigField<Boolean>

data class IntField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: Int = 0,
    val min: Int? = null,
    val max: Int? = null
) : ConfigField<Int>

data class LongField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: Long = 0,
    val min: Long? = null,
    val max: Long? = null
) : ConfigField<Long>

data class FloatField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: Float = 0.0f,
    val min: Float? = null,
    val max: Float? = null
) : ConfigField<Float>

data class DoubleField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: Double = 0.0,
    val min: Double? = null,
    val max: Double? = null
) : ConfigField<Double>

data class IntSliderField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: Int,
    val min: Int,
    val max: Int,
    val step: Int = 1
) : ConfigField<Int>

data class LongSliderField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: Long,
    val min: Long,
    val max: Long,
    val step: Long = 1L
) : ConfigField<Long>

data class StringField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: String = ""
) : ConfigField<String>

data class ChoiceField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: String,
    val options: List<String>
) : ConfigField<String>

data class ChoiceListField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: List<String> = listOf(),
    val options: List<String> = listOf()
) : ConfigField<List<String>>

data class ChoiceMapField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: Map<String, String> = mapOf(),
    val options: List<String> = listOf()
) : ConfigField<Map<String, String>>

data class KeyCodeField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: CommonKeyCode = CommonKeyCode.unknown
) : ConfigField<CommonKeyCode>

data class ColorField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: KColor = KColor.WHITE,
    val supportsAlpha: Boolean = true
) : ConfigField<KColor>

/**
 * Registry-backed selector value stored as resource location text (namespace:path).
 */
data class RegistryField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: String,
    val registryId: String? = null,
    val options: List<String> = listOf()
) : ConfigField<String>

/**
 * Nested object payload represented as simple key/value text map for v2 migration parity.
 */
data class SpecField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: Map<String, String> = mapOf()
) : ConfigField<Map<String, String>>

data class IntListField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: List<Int> = listOf()
) : ConfigField<List<Int>>

data class LongListField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: List<Long> = listOf()
) : ConfigField<List<Long>>

data class FloatListField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: List<Float> = listOf()
) : ConfigField<List<Float>>

data class DoubleListField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: List<Double> = listOf()
) : ConfigField<List<Double>>

data class StringListField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: List<String> = listOf()
) : ConfigField<List<String>>

data class ColorListField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: List<KColor> = listOf(),
    val supportsAlpha: Boolean = true
) : ConfigField<List<KColor>>

data class RegistryListField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: List<String> = listOf(),
    val registryId: String? = null,
    val options: List<String> = listOf()
) : ConfigField<List<String>>

data class KeyCodeListField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: List<CommonKeyCode> = listOf()
) : ConfigField<List<CommonKeyCode>>

data class SpecListField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: List<Map<String, String>> = listOf()
) : ConfigField<List<Map<String, String>>>

data class IntMapField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: Map<String, Int> = mapOf()
) : ConfigField<Map<String, Int>>

data class LongMapField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: Map<String, Long> = mapOf()
) : ConfigField<Map<String, Long>>

data class FloatMapField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: Map<String, Float> = mapOf()
) : ConfigField<Map<String, Float>>

data class DoubleMapField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: Map<String, Double> = mapOf()
) : ConfigField<Map<String, Double>>

data class StringMapField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: Map<String, String> = mapOf()
) : ConfigField<Map<String, String>>

data class RegistryMapField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: Map<String, String> = mapOf(),
    val registryId: String? = null,
    val options: List<String> = listOf()
) : ConfigField<Map<String, String>>

data class KeyCodeMapField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: Map<String, CommonKeyCode> = mapOf()
) : ConfigField<Map<String, CommonKeyCode>>

data class SpecMapField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: Map<String, Map<String, String>> = mapOf()
) : ConfigField<Map<String, Map<String, String>>>

