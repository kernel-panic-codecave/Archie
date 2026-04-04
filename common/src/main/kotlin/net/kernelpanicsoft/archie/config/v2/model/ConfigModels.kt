package net.kernelpanicsoft.archie.config.v2.model

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

data class StringField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: String = ""
) : ConfigField<String>

data class ColorField(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: KColor = KColor.WHITE,
    val supportsAlpha: Boolean = true
) : ConfigField<KColor>

