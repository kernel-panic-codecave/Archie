package net.kernelpanicsoft.archie.config.v2.builder

import net.kernelpanicsoft.archie.config.CommonKeyCode
import net.kernelpanicsoft.archie.config.toSnakeCase
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
import net.kernelpanicsoft.archie.config.v2.runtime.ConfigState
import net.kernelpanicsoft.archie.config.v2.runtime.ConfigV2Engine
import net.kernelpanicsoft.archie.gui.util.KColor
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * V2 document DSL with delegate-based accessors.
 *
 * Usage mirrors v1 ergonomics: define fields as delegated properties on category objects,
 * then bind once to an engine to read values anywhere in the mod.
 */
abstract class ConfigDocumentDsl(
    val id: String,
    val title: String = id
) {
    private val categories = mutableListOf<ConfigCategoryDsl>()
    private var state: ConfigState? = null

    val document: ConfigDocument by lazy {
        ConfigDocument(id = id, title = title, categories = categories.map { it.toModel() })
    }

    protected fun <T : ConfigCategoryDsl> category(category: T): T {
        categories += category
        return category
    }

    fun bind(state: ConfigState) {
        this.state = state
        categories.forEach { it.bindStateProvider { this.state } }
    }

    fun createEngine(load: Boolean = true): ConfigV2Engine {
        val engine = ConfigV2Engine(document)
        bind(engine.state)
        if (load) {
            engine.load()
        }
        return engine
    }
}

abstract class ConfigCategoryDsl(
    val id: String,
    val title: String
) {
    private val fields = mutableListOf<ConfigField<*>>()
    private val children = mutableListOf<ConfigCategoryDsl>()
    private val categoryLists = mutableMapOf<String, MutableList<ConfigCategoryDsl>>()
    private val categoryMaps = mutableMapOf<String, MutableMap<String, ConfigCategoryDsl>>()
    private var stateProvider: (() -> ConfigState?)? = null

    protected fun <T : ConfigCategoryDsl> nestedCategory(category: T): T {
        children += category
        return category
    }

    protected fun <T : ConfigCategoryDsl> categoryListOf(
        factory: () -> T
    ): PropertyDelegateProvider<ConfigCategoryDsl, ReadOnlyProperty<ConfigCategoryDsl, MutableList<T>>> {
        return PropertyDelegateProvider { _, property ->
            val key = property.name
            val list = categoryLists.getOrPut(key) { mutableListOf() }
            @Suppress("UNCHECKED_CAST")
            ReadOnlyProperty { _, _ -> list as MutableList<T> }
        }
    }

    protected fun <T : ConfigCategoryDsl> categoryMapOf(
        factory: (String) -> T
    ): PropertyDelegateProvider<ConfigCategoryDsl, ReadOnlyProperty<ConfigCategoryDsl, MutableMap<String, T>>> {
        return PropertyDelegateProvider { _, property ->
            val key = property.name
            val map = categoryMaps.getOrPut(key) { mutableMapOf() }
            @Suppress("UNCHECKED_CAST")
            ReadOnlyProperty { _, _ -> map as MutableMap<String, T> }
        }
    }

    internal fun bindStateProvider(provider: () -> ConfigState?) {
        stateProvider = provider
        children.forEach { it.bindStateProvider(provider) }
        categoryLists.values.forEach { list -> list.forEach { it.bindStateProvider(provider) } }
        categoryMaps.values.forEach { map -> map.values.forEach { it.bindStateProvider(provider) } }
    }

    internal fun toModel(): ConfigCategory {
        return ConfigCategory(
            id = id,
            title = title,
            fields = fields.toList(),
            children = (children + categoryLists.values.flatten() + categoryMaps.values.flatMap { it.values })
                .map { it.toModel() }
        )
    }

    private fun requireState(): ConfigState {
        return stateProvider?.invoke()
            ?: error("Config category '$id' is not bound. Call bind(...) or createEngine(...) first.")
    }

    private fun resolveKey(property: KProperty<*>, key: String?): String {
        return key ?: property.name.toSnakeCase()
    }

    private fun requireUniqueKey(key: String) {
        require(fields.none { it.key == key }) {
            "Duplicate config field key '$key' in category '$id'"
        }
    }

    private fun <T> define(
        key: String?,
        addField: (String) -> ConfigField<*>,
        read: ConfigState.(String) -> T
    ): PropertyDelegateProvider<ConfigCategoryDsl, ReadOnlyProperty<ConfigCategoryDsl, T>> {
        return PropertyDelegateProvider { _, property ->
            val resolved = resolveKey(property, key)
            requireUniqueKey(resolved)
            fields += addField(resolved)
            ReadOnlyProperty { _, _ -> requireState().read(resolved) }
        }
    }

    protected fun boolean(
        title: String,
        description: String? = null,
        default: Boolean = false,
        key: String? = null
    ) = define(
        key = key,
        addField = { BooleanField(it, title, description, default) },
        read = ConfigState::boolean
    )

    protected fun int(
        title: String,
        description: String? = null,
        default: Int = 0,
        min: Int? = null,
        max: Int? = null,
        key: String? = null
    ) = define(
        key = key,
        addField = { IntField(it, title, description, default, min, max) },
        read = ConfigState::int
    )

    protected fun long(
        title: String,
        description: String? = null,
        default: Long = 0L,
        min: Long? = null,
        max: Long? = null,
        key: String? = null
    ) = define(
        key = key,
        addField = { LongField(it, title, description, default, min, max) },
        read = ConfigState::long
    )

    protected fun float(
        title: String,
        description: String? = null,
        default: Float = 0f,
        min: Float? = null,
        max: Float? = null,
        key: String? = null
    ) = define(
        key = key,
        addField = { FloatField(it, title, description, default, min, max) },
        read = ConfigState::float
    )

    protected fun double(
        title: String,
        description: String? = null,
        default: Double = 0.0,
        min: Double? = null,
        max: Double? = null,
        key: String? = null
    ) = define(
        key = key,
        addField = { DoubleField(it, title, description, default, min, max) },
        read = ConfigState::double
    )

    protected fun intSlider(
        title: String,
        description: String? = null,
        default: Int,
        min: Int,
        max: Int,
        key: String? = null
    ) = define(
        key = key,
        addField = { IntSliderField(it, title, description, default, min, max) },
        read = ConfigState::int
    )

    protected fun longSlider(
        title: String,
        description: String? = null,
        default: Long,
        min: Long,
        max: Long,
        key: String? = null
    ) = define(
        key = key,
        addField = { LongSliderField(it, title, description, default, min, max) },
        read = ConfigState::long
    )

    protected fun string(
        title: String,
        description: String? = null,
        default: String = "",
        key: String? = null
    ) = define(
        key = key,
        addField = { StringField(it, title, description, default) },
        read = ConfigState::string
    )

    protected fun choice(
        title: String,
        options: List<String>,
        description: String? = null,
        default: String = options.firstOrNull().orEmpty(),
        key: String? = null
    ) = define(
        key = key,
        addField = { ChoiceField(it, title, description, default, options) },
        read = ConfigState::string
    )

    protected fun registry(
        title: String,
        description: String? = null,
        default: String,
        registryId: String? = null,
        options: List<String> = listOf(),
        key: String? = null
    ) = define(
        key = key,
        addField = { RegistryField(it, title, description, default, registryId, options) },
        read = ConfigState::registry
    )

    protected fun keyCode(
        title: String,
        description: String? = null,
        default: CommonKeyCode = CommonKeyCode.unknown,
        key: String? = null
    ) = define(
        key = key,
        addField = { KeyCodeField(it, title, description, default) },
        read = ConfigState::keyCode
    )

    protected fun color(
        title: String,
        description: String? = null,
        default: KColor = KColor.WHITE,
        supportsAlpha: Boolean = true,
        key: String? = null
    ) = define(
        key = key,
        addField = { ColorField(it, title, description, default, supportsAlpha) },
        read = ConfigState::color
    )

    protected fun intList(
        title: String,
        description: String? = null,
        default: List<Int> = listOf(),
        key: String? = null
    ) = define(
        key = key,
        addField = { IntListField(it, title, description, default) },
        read = ConfigState::intList
    )

    protected fun longList(
        title: String,
        description: String? = null,
        default: List<Long> = listOf(),
        key: String? = null
    ) = define(
        key = key,
        addField = { LongListField(it, title, description, default) },
        read = ConfigState::longList
    )

    protected fun floatList(
        title: String,
        description: String? = null,
        default: List<Float> = listOf(),
        key: String? = null
    ) = define(
        key = key,
        addField = { FloatListField(it, title, description, default) },
        read = ConfigState::floatList
    )

    protected fun doubleList(
        title: String,
        description: String? = null,
        default: List<Double> = listOf(),
        key: String? = null
    ) = define(
        key = key,
        addField = { DoubleListField(it, title, description, default) },
        read = ConfigState::doubleList
    )

    protected fun stringList(
        title: String,
        description: String? = null,
        default: List<String> = listOf(),
        key: String? = null
    ) = define(
        key = key,
        addField = { StringListField(it, title, description, default) },
        read = ConfigState::stringList
    )

    protected fun choiceList(
        title: String,
        options: List<String>,
        description: String? = null,
        default: List<String> = listOf(),
        key: String? = null
    ) = define(
        key = key,
        addField = { ChoiceListField(it, title, description, default, options) },
        read = ConfigState::choiceList
    )

    protected fun registryList(
        title: String,
        description: String? = null,
        default: List<String> = listOf(),
        registryId: String? = null,
        options: List<String> = listOf(),
        key: String? = null
    ) = define(
        key = key,
        addField = { RegistryListField(it, title, description, default, registryId, options) },
        read = ConfigState::registryList
    )

    protected fun keyCodeList(
        title: String,
        description: String? = null,
        default: List<CommonKeyCode> = listOf(),
        key: String? = null
    ) = define(
        key = key,
        addField = { KeyCodeListField(it, title, description, default) },
        read = ConfigState::keyCodeList
    )

    protected fun colorList(
        title: String,
        description: String? = null,
        default: List<KColor> = listOf(),
        supportsAlpha: Boolean = true,
        key: String? = null
    ) = define(
        key = key,
        addField = { ColorListField(it, title, description, default, supportsAlpha) },
        read = ConfigState::colorList
    )

    protected fun intMap(
        title: String,
        description: String? = null,
        default: Map<String, Int> = mapOf(),
        key: String? = null
    ) = define(
        key = key,
        addField = { IntMapField(it, title, description, default) },
        read = ConfigState::intMap
    )

    protected fun longMap(
        title: String,
        description: String? = null,
        default: Map<String, Long> = mapOf(),
        key: String? = null
    ) = define(
        key = key,
        addField = { LongMapField(it, title, description, default) },
        read = ConfigState::longMap
    )

    protected fun floatMap(
        title: String,
        description: String? = null,
        default: Map<String, Float> = mapOf(),
        key: String? = null
    ) = define(
        key = key,
        addField = { FloatMapField(it, title, description, default) },
        read = ConfigState::floatMap
    )

    protected fun doubleMap(
        title: String,
        description: String? = null,
        default: Map<String, Double> = mapOf(),
        key: String? = null
    ) = define(
        key = key,
        addField = { DoubleMapField(it, title, description, default) },
        read = ConfigState::doubleMap
    )

    protected fun stringMap(
        title: String,
        description: String? = null,
        default: Map<String, String> = mapOf(),
        key: String? = null
    ) = define(
        key = key,
        addField = { StringMapField(it, title, description, default) },
        read = ConfigState::stringMap
    )

    protected fun choiceMap(
        title: String,
        options: List<String>,
        description: String? = null,
        default: Map<String, String> = mapOf(),
        key: String? = null
    ) = define(
        key = key,
        addField = { ChoiceMapField(it, title, description, default, options) },
        read = ConfigState::choiceMap
    )

    protected fun registryMap(
        title: String,
        description: String? = null,
        default: Map<String, String> = mapOf(),
        registryId: String? = null,
        options: List<String> = listOf(),
        key: String? = null
    ) = define(
        key = key,
        addField = { RegistryMapField(it, title, description, default, registryId, options) },
        read = ConfigState::registryMap
    )

    protected fun keyCodeMap(
        title: String,
        description: String? = null,
        default: Map<String, CommonKeyCode> = mapOf(),
        key: String? = null
    ) = define(
        key = key,
        addField = { KeyCodeMapField(it, title, description, default) },
        read = ConfigState::keyCodeMap
    )
}






