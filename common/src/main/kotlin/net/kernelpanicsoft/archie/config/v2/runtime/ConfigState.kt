package net.kernelpanicsoft.archie.config.v2.runtime

import net.kernelpanicsoft.archie.config.v2.model.BooleanField
import net.kernelpanicsoft.archie.config.v2.model.ColorField
import net.kernelpanicsoft.archie.config.v2.model.ConfigCategory
import net.kernelpanicsoft.archie.config.v2.model.ConfigDocument
import net.kernelpanicsoft.archie.config.v2.model.ConfigField
import net.kernelpanicsoft.archie.config.v2.model.IntField
import net.kernelpanicsoft.archie.config.v2.model.StringField
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

    fun setBoolean(key: String, value: Boolean) {
        values[key] = value
    }

    fun setInt(key: String, value: Int) {
        values[key] = value
    }

    fun setString(key: String, value: String) {
        values[key] = value
    }

    fun setColor(key: String, value: KColor) {
        values[key] = value
    }

    fun boolean(key: String): Boolean = typed(key)
    fun int(key: String): Int = typed(key)
    fun string(key: String): String = typed(key)
    fun color(key: String): KColor = typed(key)

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
            is StringField -> if (values[field.key] !is String) errors += "${field.key} is not String"
            is ColorField -> if (values[field.key] !is KColor) errors += "${field.key} is not KColor"
        }
    }
}

