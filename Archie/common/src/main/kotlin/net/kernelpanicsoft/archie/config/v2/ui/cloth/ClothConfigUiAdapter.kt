package net.kernelpanicsoft.archie.config.v2.ui.cloth

import java.util.function.Consumer
import java.util.function.Supplier
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry
import me.shedaniel.clothconfig2.api.ConfigBuilder
import me.shedaniel.clothconfig2.api.ConfigCategory
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.api.ModifierKeyCode
import me.shedaniel.clothconfig2.gui.entries.SubCategoryListEntry
import me.shedaniel.math.Color
import net.kernelpanicsoft.archie.config.toClient
import net.kernelpanicsoft.archie.config.toCommon
import net.kernelpanicsoft.archie.config.v2.model.BooleanField
import net.kernelpanicsoft.archie.config.v2.model.ChoiceField
import net.kernelpanicsoft.archie.config.v2.model.ChoiceListField
import net.kernelpanicsoft.archie.config.v2.model.ChoiceMapField
import net.kernelpanicsoft.archie.config.v2.model.ColorField
import net.kernelpanicsoft.archie.config.v2.model.ColorListField
import net.kernelpanicsoft.archie.config.v2.model.ConfigCategory as V2Category
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
import net.kernelpanicsoft.archie.config.v2.runtime.ConfigRegistryOptions
import net.kernelpanicsoft.archie.config.v2.ui.ConfigUiAdapter
import net.kernelpanicsoft.archie.config.v2.ui.ConfigUiAdapterIds
import net.kernelpanicsoft.archie.config.v2.ui.ConfigUiAdapters
import net.kernelpanicsoft.archie.config.v2.builder.startColorList
import net.kernelpanicsoft.archie.config.v2.builder.startDoubleMap
import net.kernelpanicsoft.archie.config.v2.builder.startFloatMap
import net.kernelpanicsoft.archie.config.v2.builder.startIntMap
import net.kernelpanicsoft.archie.config.v2.builder.startKeycodeList
import net.kernelpanicsoft.archie.config.v2.builder.startKeycodeMap
import net.kernelpanicsoft.archie.config.v2.builder.startLongMap
import net.kernelpanicsoft.archie.config.v2.builder.startStrMap
import net.kernelpanicsoft.archie.config.v2.builder.startStringChoiceList
import net.kernelpanicsoft.archie.config.v2.builder.startStringChoiceMap
import net.kernelpanicsoft.archie.config.v2.builder.startStringDropdownField
import net.kernelpanicsoft.archie.gui.util.KColor
import net.kernelpanicsoft.archie.util.MutableEntry
import net.kernelpanicsoft.archie.util.toMutableEntry
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/**
 * Cloth-backed renderer for v2 config documents.
 */
object ClothConfigUiAdapter : ConfigUiAdapter {
    override val id: String = ConfigUiAdapterIds.CLOTH

    private const val CLOTH_ROOT_CLASS = "me.shedaniel.clothconfig2.api.ConfigBuilder"

    private val json = Json { ignoreUnknownKeys = true }

    override fun buildScreen(document: ConfigDocument, state: ConfigState): (Screen) -> Screen
    {
        val builder = ConfigBuilder.create()
        builder.title = Component.literal(document.title)

        // State is mutated as users edit controls; saving behavior is owned by caller.
        builder.savingRunnable = Runnable { }

        val entryBuilder = builder.entryBuilder()
        document.categories.forEach { category ->
            addRootCategory(builder, entryBuilder, category, state)
        }
        return { parent -> builder.parentScreen = parent; builder.build() }
    }

    private fun KColor.toArgbInt(): Int {
        return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    }

    private fun Int.toKColor(): KColor {
        return KColor(
            red = (this shr 16) and 0xFF,
            green = (this shr 8) and 0xFF,
            blue = this and 0xFF,
            alpha = (this shr 24) and 0xFF
        )
    }

    private fun encodeSpecMap(value: Map<String, String>): String {
        return json.encodeToString(MapSerializer(String.serializer(), String.serializer()), value)
    }

    private fun decodeSpecMap(raw: String): Map<String, String> {
        return runCatching {
            json.decodeFromString(MapSerializer(String.serializer(), String.serializer()), raw)
        }.getOrDefault(mapOf())
    }


    private fun addRootCategory(
        builder: ConfigBuilder,
        entryBuilder: ConfigEntryBuilder,
        category: V2Category,
        state: ConfigState
    ) {
        val clothCategory = builder.getOrCreateCategory(Component.literal(category.title))
        addFieldsToCategory(clothCategory, entryBuilder, category.fields, state)

        category.children.forEach { child ->
            clothCategory.addEntry(buildSubCategory(entryBuilder, child, state))
        }
    }

    private fun buildSubCategory(
        entryBuilder: ConfigEntryBuilder,
        category: V2Category,
        state: ConfigState
    ): SubCategoryListEntry {
        val sub = entryBuilder.startSubCategory(Component.literal(category.title))

        addFieldsToSubcategory(sub, entryBuilder, category.fields, state)

        category.children.forEach { child ->
            sub.add(buildSubCategory(entryBuilder, child, state))
        }

        return sub.build()
    }

    private fun addFieldsToCategory(
        category: ConfigCategory,
        entryBuilder: ConfigEntryBuilder,
        fields: List<ConfigField<*>>,
        state: ConfigState
    ) {
        fields.forEach { field ->
            category.addEntry(renderField(entryBuilder, field, state))
        }
    }

    private fun addFieldsToSubcategory(
        category: me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder,
        entryBuilder: ConfigEntryBuilder,
        fields: List<ConfigField<*>>,
        state: ConfigState
    ) {
        fields.forEach { field ->
            category.add(renderField(entryBuilder, field, state))
        }
    }

    private fun renderField(
        entryBuilder: ConfigEntryBuilder,
        field: ConfigField<*>,
        state: ConfigState
    ): AbstractConfigListEntry<*> {
        return when (field) {
            is BooleanField -> entryBuilder.startBooleanToggle(Component.literal(field.title), state.boolean(field.key))
                .apply {
                    saveConsumer = Consumer { state.setBoolean(field.key, it) }
                    defaultValue = Supplier { field.defaultValue }
                }
                .build()

            is IntField -> entryBuilder.startIntField(Component.literal(field.title), state.int(field.key))
                .apply {
                    saveConsumer = Consumer { state.setInt(field.key, it) }
                    defaultValue = Supplier { field.defaultValue }
                    if (field.min != null) setMin(field.min)
                    if (field.max != null) setMax(field.max)
                }
                .build()

            is LongField -> entryBuilder.startLongField(Component.literal(field.title), state.long(field.key))
                .apply {
                    saveConsumer = Consumer { state.setLong(field.key, it) }
                    defaultValue = Supplier { field.defaultValue }
                    if (field.min != null) setMin(field.min)
                    if (field.max != null) setMax(field.max)
                }
                .build()

            is FloatField -> entryBuilder.startFloatField(Component.literal(field.title), state.float(field.key))
                .apply {
                    saveConsumer = Consumer { state.setFloat(field.key, it) }
                    defaultValue = Supplier { field.defaultValue }
                }
                .build()

            is DoubleField -> entryBuilder.startDoubleField(Component.literal(field.title), state.double(field.key))
                .apply {
                    saveConsumer = Consumer { state.setDouble(field.key, it) }
                    defaultValue = Supplier { field.defaultValue }
                }
                .build()

            is IntSliderField -> entryBuilder.startIntSlider(Component.literal(field.title), state.int(field.key), field.min, field.max)
                .apply {
                    saveConsumer = Consumer { state.setInt(field.key, it) }
                    defaultValue = Supplier { field.defaultValue }
                }
                .build()

            is LongSliderField -> entryBuilder.startLongSlider(Component.literal(field.title), state.long(field.key), field.min, field.max)
                .apply {
                    saveConsumer = Consumer { state.setLong(field.key, it) }
                    defaultValue = Supplier { field.defaultValue }
                }
                .build()

            is StringField -> entryBuilder.startStrField(Component.literal(field.title), state.string(field.key))
                .apply {
                    saveConsumer = Consumer { state.setString(field.key, it) }
                    defaultValue = Supplier { field.defaultValue }
                }
                .build()

            is ChoiceField -> entryBuilder
                .let {
                    if (field.options.isNotEmpty()) {
                        it.startStringDropdownField(
                            Component.literal(field.title),
                            state.string(field.key),
                            field.options
                        )
                    } else {
                        it.startStrField(Component.literal(field.title), state.string(field.key))
                    }
                }
                .apply {
                    saveConsumer = Consumer { state.setString(field.key, it) }
                    defaultValue = Supplier { field.defaultValue }
                }
                .build()

            is KeyCodeField -> entryBuilder.startModifierKeyCodeField(Component.literal(field.title), state.keyCode(field.key).toClient())
                .apply {
                    setModifierSaveConsumer { state.setKeyCode(field.key, it.toCommon()) }
                    setModifierDefaultValue { field.defaultValue.toClient() }
                }
                .build()

            is RegistryField -> {
                val options = if (field.options.isNotEmpty()) {
                    field.options
                } else {
                    ConfigRegistryOptions.optionsFor(
                        field.registryId,
                        candidates = listOf(state.registry(field.key), field.defaultValue)
                    )
                }

                if (options.isNotEmpty()) {
                    entryBuilder.startStringDropdownField(
                        Component.literal(field.title),
                        state.registry(field.key),
                        options
                    )
                        .apply {
                            saveConsumer = Consumer { state.setRegistry(field.key, it) }
                            defaultValue = Supplier { field.defaultValue }
                        }
                        .build()
                } else {
                    entryBuilder.startStrField(Component.literal(field.title), state.registry(field.key))
                        .apply {
                            saveConsumer = Consumer { state.setRegistry(field.key, it) }
                            defaultValue = Supplier { field.defaultValue }
                        }
                        .build()
                }
            }

            is SpecField -> {
                entryBuilder.startStrMap(Component.literal(field.title), state.spec(field.key))
                    .apply {
                        saveConsumer = Consumer<List<MutableEntry<String, String>>> { value ->
                            state.setSpec(field.key, value.associate { it.toPair() })
                        }
                        defaultValue = Supplier { field.defaultValue.entries.toList().map { it.toMutableEntry() } }
                    }
                    .build()
            }

            is ColorField -> entryBuilder.startColorField(Component.literal(field.title), state.color(field.key).toArgbInt())
                .apply {
                    setAlphaMode(field.supportsAlpha)
                    setSaveConsumer { state.setColor(field.key, it.toKColor()) }
                    setDefaultValue { field.defaultValue.toArgbInt() }
                }
                .build()

            is IntListField -> entryBuilder.startIntList(Component.literal(field.title), state.intList(field.key))
                .apply {
                    saveConsumer = Consumer { state.setIntList(field.key, it) }
                    defaultValue = Supplier { field.defaultValue }
                }
                .build()

            is LongListField -> entryBuilder.startLongList(Component.literal(field.title), state.longList(field.key))
                .apply {
                    saveConsumer = Consumer { state.setLongList(field.key, it) }
                    defaultValue = Supplier { field.defaultValue }
                }
                .build()

            is FloatListField -> entryBuilder.startFloatList(Component.literal(field.title), state.floatList(field.key))
                .apply {
                    saveConsumer = Consumer { state.setFloatList(field.key, it) }
                    defaultValue = Supplier { field.defaultValue }
                }
                .build()

            is DoubleListField -> entryBuilder.startDoubleList(Component.literal(field.title), state.doubleList(field.key))
                .apply {
                    saveConsumer = Consumer { state.setDoubleList(field.key, it) }
                    defaultValue = Supplier { field.defaultValue }
                }
                .build()

            is StringListField -> entryBuilder.startStrList(Component.literal(field.title), state.stringList(field.key))
                .apply {
                    saveConsumer = Consumer { state.setStringList(field.key, it) }
                    defaultValue = Supplier { field.defaultValue }
                }
                .build()

            is ChoiceListField -> {
                val options = field.options
                if (options.isNotEmpty()) {
                    entryBuilder.startStringChoiceList(
                        Component.literal(field.title),
                        state.choiceList(field.key),
                        options
                    )
                        .apply {
                            saveConsumer = Consumer { state.setChoiceList(field.key, it) }
                            defaultValue = Supplier { field.defaultValue }
                        }
                        .build()
                } else {
                    entryBuilder.startStrList(Component.literal(field.title), state.choiceList(field.key))
                        .apply {
                            saveConsumer = Consumer { state.setChoiceList(field.key, it) }
                            defaultValue = Supplier { field.defaultValue }
                        }
                        .build()
                }
            }

            is RegistryListField -> {
                val options = if (field.options.isNotEmpty()) {
                    field.options
                } else {
                    ConfigRegistryOptions.optionsFor(
                        field.registryId,
                        candidates = state.registryList(field.key) + field.defaultValue
                    )
                }
                if (options.isNotEmpty()) {
                    entryBuilder.startStringChoiceList(
                        Component.literal(field.title),
                        state.registryList(field.key),
                        options,
                    ) { options.first() }
                        .apply {
                            saveConsumer = Consumer { state.setRegistryList(field.key, it) }
                            defaultValue = Supplier { field.defaultValue }
                        }
                        .build()
                } else {
                    entryBuilder.startStrList(Component.literal(field.title), state.registryList(field.key))
                        .apply {
                            saveConsumer = Consumer { state.setRegistryList(field.key, it) }
                            defaultValue = Supplier { field.defaultValue }
                        }
                        .build()
                }
            }

            is KeyCodeListField -> entryBuilder.startKeycodeList(
                Component.literal(field.title),
                state.keyCodeList(field.key).map { it.toClient() }
            ) { ModifierKeyCode.unknown() }
                .apply {
                    saveConsumer = Consumer { value -> state.setKeyCodeList(field.key, value.map { it.toCommon() }) }
                    defaultValue = Supplier { field.defaultValue.map { it.toClient() } }
                }
                .build()

            is SpecListField -> {
                entryBuilder.startStrList(
                    Component.literal(field.title),
                    state.specList(field.key).map(::encodeSpecMap)
                )
                    .apply {
                        saveConsumer = Consumer { value ->
                            state.setSpecList(field.key, value.map(::decodeSpecMap))
                        }
                        defaultValue = Supplier { field.defaultValue.map(::encodeSpecMap) }
                    }
                    .build()
            }

            is ColorListField -> entryBuilder.startColorList(
                Component.literal(field.title),
                state.colorList(field.key).map { Color.ofTransparent(it.toArgbInt()) }
            ) {
                val base = field.defaultValue.firstOrNull() ?: KColor.WHITE
                Color.ofTransparent(base.toArgbInt())
            }
                .apply {
                    alphaMode = field.supportsAlpha
                    setSaveConsumer { value ->
                        val colors = if (field.supportsAlpha) {
                            value.map { it.toKColor() }
                        } else {
                            value.map { Color.ofOpaque(it).color.toKColor() }
                        }
                        state.setColorList(field.key, colors)
                    }
                    setDefaultValue { field.defaultValue.map { it.toArgbInt() } }
                }
                .build()

            is IntMapField -> entryBuilder.startIntMap(Component.literal(field.title), state.intMap(field.key))
                .apply {
                    saveConsumer = Consumer<List<MutableEntry<String, Int>>> { value ->
                        state.setIntMap(field.key, value.associate { it.toPair() })
                    }
                    defaultValue = Supplier { field.defaultValue.entries.toList().map { it.toMutableEntry() } }
                }
                .build()

            is LongMapField -> entryBuilder.startLongMap(Component.literal(field.title), state.longMap(field.key))
                .apply {
                    saveConsumer = Consumer<List<MutableEntry<String, Long>>> { value ->
                        state.setLongMap(field.key, value.associate { it.toPair() })
                    }
                    defaultValue = Supplier { field.defaultValue.entries.toList().map { it.toMutableEntry() } }
                }
                .build()

            is FloatMapField -> entryBuilder.startFloatMap(Component.literal(field.title), state.floatMap(field.key))
                .apply {
                    saveConsumer = Consumer<List<MutableEntry<String, Float>>> { value ->
                        state.setFloatMap(field.key, value.associate { it.toPair() })
                    }
                    defaultValue = Supplier { field.defaultValue.entries.toList().map { it.toMutableEntry() } }
                }
                .build()

            is DoubleMapField -> entryBuilder.startDoubleMap(Component.literal(field.title), state.doubleMap(field.key))
                .apply {
                    saveConsumer = Consumer<List<MutableEntry<String, Double>>> { value ->
                        state.setDoubleMap(field.key, value.associate { it.toPair() })
                    }
                    defaultValue = Supplier { field.defaultValue.entries.toList().map { it.toMutableEntry() } }
                }
                .build()

            is StringMapField -> entryBuilder.startStrMap(Component.literal(field.title), state.stringMap(field.key))
                .apply {
                    saveConsumer = Consumer<List<MutableEntry<String, String>>> { value ->
                        state.setStringMap(field.key, value.associate { it.toPair() })
                    }
                    defaultValue = Supplier { field.defaultValue.entries.toList().map { it.toMutableEntry() } }
                }
                .build()

            is ChoiceMapField -> {
                val options = field.options
                if (options.isNotEmpty()) {
                    entryBuilder.startStringChoiceMap(
                        Component.literal(field.title),
                        state.choiceMap(field.key),
                        options
                    )
                        .apply {
                            saveConsumer = Consumer<List<MutableEntry<String, String>>> { value ->
                                state.setChoiceMap(field.key, value.associate { it.toPair() })
                            }
                            defaultValue = Supplier { field.defaultValue.entries.toList().map { it.toMutableEntry() } }
                        }
                        .build()
                } else {
                    entryBuilder.startStrMap(Component.literal(field.title), state.choiceMap(field.key))
                        .apply {
                            saveConsumer = Consumer<List<MutableEntry<String, String>>> { value ->
                                state.setChoiceMap(field.key, value.associate { it.toPair() })
                            }
                            defaultValue = Supplier { field.defaultValue.entries.toList().map { it.toMutableEntry() } }
                        }
                        .build()
                }
            }

            is RegistryMapField -> {
                val options = if (field.options.isNotEmpty()) {
                    field.options
                } else {
                    ConfigRegistryOptions.optionsFor(
                        field.registryId,
                        candidates = state.registryMap(field.key).values + field.defaultValue.values
                    )
                }
                if (options.isNotEmpty()) {
                    entryBuilder.startStringChoiceMap(
                        Component.literal(field.title),
                        state.registryMap(field.key),
                        options,
                    ) { options.first() }
                        .apply {
                            saveConsumer = Consumer<List<MutableEntry<String, String>>> { value ->
                                state.setRegistryMap(field.key, value.associate { it.toPair() })
                            }
                            defaultValue = Supplier { field.defaultValue.entries.toList().map { it.toMutableEntry() } }
                        }
                        .build()
                } else {
                    entryBuilder.startStrMap(Component.literal(field.title), state.registryMap(field.key))
                        .apply {
                            saveConsumer = Consumer<List<MutableEntry<String, String>>> { value ->
                                state.setRegistryMap(field.key, value.associate { it.toPair() })
                            }
                            defaultValue = Supplier { field.defaultValue.entries.toList().map { it.toMutableEntry() } }
                        }
                        .build()
                }
            }

            is KeyCodeMapField -> entryBuilder.startKeycodeMap(
                Component.literal(field.title),
                state.keyCodeMap(field.key).mapValues { it.value.toClient() }
            ) { ModifierKeyCode.unknown() }
                .apply {
                    saveConsumer = Consumer { value ->
                        state.setKeyCodeMap(field.key, value.associate { it.toPair() }.mapValues { it.value.toCommon() })
                    }
                    defaultValue = Supplier {
                        field.defaultValue.mapValues { it.value.toClient() }.entries.toList().map { it.toMutableEntry() }
                    }
                }
                .build()

            is SpecMapField -> {
                entryBuilder.startStrMap(
                    Component.literal(field.title),
                    state.specMap(field.key).mapValues { encodeSpecMap(it.value) }
                )
                    .apply {
                        saveConsumer = Consumer<List<MutableEntry<String, String>>> { value ->
                            state.setSpecMap(
                                field.key,
                                value.associate { it.key to decodeSpecMap(it.value) }
                            )
                        }
                        defaultValue = Supplier {
                            field.defaultValue
                                .mapValues { encodeSpecMap(it.value) }
                                .entries
                                .toList()
                                .map { it.toMutableEntry() }
                        }
                    }
                    .build()
            }
        }
    }
}


