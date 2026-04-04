package net.kernelpanicsoft.archie.config.v2.runtime

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

/**
 * Mutable runtime state for a [ConfigDocument].
 */
class ConfigState(private val document: ConfigDocument) {
    private val values = mutableMapOf<String, Any>()

    init {
        seedDefaults(document.categories)
    }

    private fun seedDefaults(categories: List<ConfigCategory>) {
        categories.forEach { category ->
            category.fields.forEach { field -> values[field.key] = field.defaultValue as Any }
            seedDefaults(category.children)
        }
    }

    fun allValues(): Map<String, Any> = values.toMap()

    fun setValue(key: String, value: Any) {
        values[key] = value
    }

    fun setBoolean(key: String, value: Boolean) {
        values[key] = value
    }

    fun setInt(key: String, value: Int) {
        values[key] = value
    }

    fun setLong(key: String, value: Long) {
        values[key] = value
    }

    fun setFloat(key: String, value: Float) {
        values[key] = value
    }

    fun setDouble(key: String, value: Double) {
        values[key] = value
    }

    fun setString(key: String, value: String) {
        values[key] = value
    }

    fun setRegistry(key: String, value: String) {
        values[key] = value
    }

    fun setKeyCode(key: String, value: CommonKeyCode) {
        values[key] = value
    }

    fun setSpec(key: String, value: Map<String, String>) {
        values[key] = value
    }

    fun setIntList(key: String, value: List<Int>) {
        values[key] = value
    }

    fun setLongList(key: String, value: List<Long>) {
        values[key] = value
    }

    fun setFloatList(key: String, value: List<Float>) {
        values[key] = value
    }

    fun setDoubleList(key: String, value: List<Double>) {
        values[key] = value
    }

    fun setStringList(key: String, value: List<String>) {
        values[key] = value
    }

    fun setRegistryList(key: String, value: List<String>) {
        values[key] = value
    }

    fun setChoiceList(key: String, value: List<String>) {
        values[key] = value
    }

    fun setKeyCodeList(key: String, value: List<CommonKeyCode>) {
        values[key] = value
    }

    fun setSpecList(key: String, value: List<Map<String, String>>) {
        values[key] = value
    }

    fun setIntMap(key: String, value: Map<String, Int>) {
        values[key] = value
    }

    fun setLongMap(key: String, value: Map<String, Long>) {
        values[key] = value
    }

    fun setFloatMap(key: String, value: Map<String, Float>) {
        values[key] = value
    }

    fun setDoubleMap(key: String, value: Map<String, Double>) {
        values[key] = value
    }

    fun setStringMap(key: String, value: Map<String, String>) {
        values[key] = value
    }

    fun setRegistryMap(key: String, value: Map<String, String>) {
        values[key] = value
    }

    fun setChoiceMap(key: String, value: Map<String, String>) {
        values[key] = value
    }

    fun setKeyCodeMap(key: String, value: Map<String, CommonKeyCode>) {
        values[key] = value
    }

    fun setSpecMap(key: String, value: Map<String, Map<String, String>>) {
        values[key] = value
    }

    fun setColor(key: String, value: KColor) {
        values[key] = value
    }

    fun setColorList(key: String, value: List<KColor>) {
        values[key] = value
    }

    fun boolean(key: String): Boolean = typed(key)
    fun int(key: String): Int = typed(key)
    fun long(key: String): Long = typed(key)
    fun float(key: String): Float = typed(key)
    fun double(key: String): Double = typed(key)
    fun string(key: String): String = typed(key)
    fun registry(key: String): String = typed(key)
    fun keyCode(key: String): CommonKeyCode = typed(key)
    fun spec(key: String): Map<String, String> = typed(key)
    fun color(key: String): KColor = typed(key)
    fun colorList(key: String): List<KColor> = typed(key)
    fun intList(key: String): List<Int> = typed(key)
    fun longList(key: String): List<Long> = typed(key)
    fun floatList(key: String): List<Float> = typed(key)
    fun doubleList(key: String): List<Double> = typed(key)
    fun stringList(key: String): List<String> = typed(key)
    fun choiceList(key: String): List<String> = typed(key)
    fun registryList(key: String): List<String> = typed(key)
    fun keyCodeList(key: String): List<CommonKeyCode> = typed(key)
    fun specList(key: String): List<Map<String, String>> = typed(key)
    fun intMap(key: String): Map<String, Int> = typed(key)
    fun longMap(key: String): Map<String, Long> = typed(key)
    fun floatMap(key: String): Map<String, Float> = typed(key)
    fun doubleMap(key: String): Map<String, Double> = typed(key)
    fun stringMap(key: String): Map<String, String> = typed(key)
    fun choiceMap(key: String): Map<String, String> = typed(key)
    fun registryMap(key: String): Map<String, String> = typed(key)
    fun keyCodeMap(key: String): Map<String, CommonKeyCode> = typed(key)
    fun specMap(key: String): Map<String, Map<String, String>> = typed(key)

    @Suppress("UNCHECKED_CAST")
    private fun <T> typed(key: String): T {
        return values[key] as? T
            ?: error("No value for key '$key' or value has wrong type")
    }

    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        validateCategories(document.categories, errors)
        return errors
    }

    private fun validateCategories(categories: List<ConfigCategory>, errors: MutableList<String>) {
        categories.forEach { category ->
            category.fields.forEach { field -> validateField(field, errors) }
            validateCategories(category.children, errors)
        }
    }

    private fun validateField(field: ConfigField<*>, errors: MutableList<String>) {
        when (field) {
            is BooleanField -> if (values[field.key] !is Boolean) errors += "${field.key} is not Boolean"
            is IntField -> {
                val value = values[field.key]
                if (value !is Int) {
                    errors += "${field.key} is not Int"
                    return
                }
                if (field.min != null && value < field.min) errors += "${field.key} < min ${field.min}"
                if (field.max != null && value > field.max) errors += "${field.key} > max ${field.max}"
            }
            is LongField -> {
                val value = values[field.key]
                if (value !is Long) {
                    errors += "${field.key} is not Long"
                    return
                }
                if (field.min != null && value < field.min) errors += "${field.key} < min ${field.min}"
                if (field.max != null && value > field.max) errors += "${field.key} > max ${field.max}"
            }
            is FloatField -> {
                val value = values[field.key]
                if (value !is Float) {
                    errors += "${field.key} is not Float"
                    return
                }
                if (field.min != null && value < field.min) errors += "${field.key} < min ${field.min}"
                if (field.max != null && value > field.max) errors += "${field.key} > max ${field.max}"
            }
            is DoubleField -> {
                val value = values[field.key]
                if (value !is Double) {
                    errors += "${field.key} is not Double"
                    return
                }
                if (field.min != null && value < field.min) errors += "${field.key} < min ${field.min}"
                if (field.max != null && value > field.max) errors += "${field.key} > max ${field.max}"
            }
            is IntSliderField -> {
                val value = values[field.key]
                if (value !is Int) {
                    errors += "${field.key} is not Int"
                    return
                }
                if (value < field.min) errors += "${field.key} < min ${field.min}"
                if (value > field.max) errors += "${field.key} > max ${field.max}"
            }
            is LongSliderField -> {
                val value = values[field.key]
                if (value !is Long) {
                    errors += "${field.key} is not Long"
                    return
                }
                if (value < field.min) errors += "${field.key} < min ${field.min}"
                if (value > field.max) errors += "${field.key} > max ${field.max}"
            }
            is StringField -> if (values[field.key] !is String) errors += "${field.key} is not String"
            is RegistryField -> if (values[field.key] !is String) errors += "${field.key} is not Registry<String>"
            is KeyCodeField -> if (values[field.key] !is CommonKeyCode) errors += "${field.key} is not CommonKeyCode"
            is SpecField -> if (values[field.key] !is Map<*, *>) errors += "${field.key} is not Spec<Map<String,String>>"
            is ChoiceField -> {
                val value = values[field.key]
                if (value !is String) {
                    errors += "${field.key} is not String"
                    return
                }
                if (field.options.isNotEmpty() && value !in field.options) {
                    errors += "${field.key} is not in options"
                }
            }
            is ChoiceListField -> {
                val value = values[field.key]
                if (value !is List<*>) {
                    errors += "${field.key} is not List<String>"
                    return
                }
                if (field.options.isNotEmpty() && value.any { it !is String || it !in field.options }) {
                    errors += "${field.key} has value outside options"
                }
            }
            is ChoiceMapField -> {
                val value = values[field.key]
                if (value !is Map<*, *>) {
                    errors += "${field.key} is not Map<String, String>"
                    return
                }
                if (field.options.isNotEmpty() && value.values.any { it !is String || it !in field.options }) {
                    errors += "${field.key} has value outside options"
                }
            }
            is ColorField -> if (values[field.key] !is KColor) errors += "${field.key} is not KColor"
            is ColorListField -> if (values[field.key] !is List<*>) errors += "${field.key} is not List<KColor>"
            is IntListField -> if (values[field.key] !is List<*>) errors += "${field.key} is not List<Int>"
            is LongListField -> if (values[field.key] !is List<*>) errors += "${field.key} is not List<Long>"
            is FloatListField -> if (values[field.key] !is List<*>) errors += "${field.key} is not List<Float>"
            is DoubleListField -> if (values[field.key] !is List<*>) errors += "${field.key} is not List<Double>"
            is StringListField -> if (values[field.key] !is List<*>) errors += "${field.key} is not List<String>"
            is RegistryListField -> if (values[field.key] !is List<*>) errors += "${field.key} is not RegistryList<List<String>>"
            is KeyCodeListField -> {
                val value = values[field.key]
                if (value !is List<*> || value.any { it !is CommonKeyCode }) {
                    errors += "${field.key} is not List<CommonKeyCode>"
                }
            }
            is SpecListField -> if (values[field.key] !is List<*>) errors += "${field.key} is not SpecList<List<Map<String,String>>>"
            is IntMapField -> if (values[field.key] !is Map<*, *>) errors += "${field.key} is not Map<String, Int>"
            is LongMapField -> if (values[field.key] !is Map<*, *>) errors += "${field.key} is not Map<String, Long>"
            is FloatMapField -> if (values[field.key] !is Map<*, *>) errors += "${field.key} is not Map<String, Float>"
            is DoubleMapField -> if (values[field.key] !is Map<*, *>) errors += "${field.key} is not Map<String, Double>"
            is StringMapField -> if (values[field.key] !is Map<*, *>) errors += "${field.key} is not Map<String, String>"
            is RegistryMapField -> if (values[field.key] !is Map<*, *>) errors += "${field.key} is not RegistryMap<Map<String,String>>"
            is KeyCodeMapField -> {
                val value = values[field.key]
                if (value !is Map<*, *> || value.keys.any { it !is String } || value.values.any { it !is CommonKeyCode }) {
                    errors += "${field.key} is not Map<String, CommonKeyCode>"
                }
            }
            is SpecMapField -> if (values[field.key] !is Map<*, *>) errors += "${field.key} is not SpecMap<Map<String,Map<String,String>>>"
        }
    }
}

