package net.kernelpanicsoft.archie.config.v2.ui.yacl

import dev.isxander.yacl3.api.Binding
import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.ListOption
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.YetAnotherConfigLib
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder
import dev.isxander.yacl3.api.controller.ControllerBuilder
import dev.isxander.yacl3.api.controller.CyclingListControllerBuilder
import dev.isxander.yacl3.api.controller.DoubleFieldControllerBuilder
import dev.isxander.yacl3.api.controller.DropdownStringControllerBuilder
import dev.isxander.yacl3.api.controller.FloatFieldControllerBuilder
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder
import dev.isxander.yacl3.api.controller.LongFieldControllerBuilder
import dev.isxander.yacl3.api.controller.LongSliderControllerBuilder
import dev.isxander.yacl3.api.controller.StringControllerBuilder
import java.util.function.Supplier
import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.config.v2.model.BooleanField
import net.kernelpanicsoft.archie.config.v2.model.ChoiceField
import net.kernelpanicsoft.archie.config.v2.model.ChoiceListField
import net.kernelpanicsoft.archie.config.v2.model.ChoiceMapField
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
import net.kernelpanicsoft.archie.config.v2.runtime.ConfigRegistryOptions
import net.kernelpanicsoft.archie.config.v2.runtime.ConfigState
import net.kernelpanicsoft.archie.config.v2.serializer.ConfigValueCodec
import net.kernelpanicsoft.archie.config.v2.ui.ConfigUiAdapter
import net.kernelpanicsoft.archie.config.v2.ui.ConfigUiAdapterIds
import net.kernelpanicsoft.archie.config.v2.ui.ConfigUiAdapters
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/**
 * YACL adapter implemented with direct API calls.
 */
object YaclConfigUiAdapter : ConfigUiAdapter {
    override val id: String = ConfigUiAdapterIds.YACL

    private const val YACL_ROOT_CLASS = "dev.isxander.yacl3.api.YetAnotherConfigLib"

    private fun normalizeText(value: String?): String {
        val trimmed = value?.trim().orEmpty()
        return if (trimmed.length >= 2 && trimmed.startsWith('"') && trimmed.endsWith('"')) {
            trimmed.substring(1, trimmed.length - 1)
        } else {
            trimmed
        }
    }

    private fun normalizedCyclingValues(options: Iterable<String?>, vararg candidates: String?): List<String> {
        return (options.asSequence().map(::normalizeText) + candidates.asSequence().map(::normalizeText))
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }

    private fun selectCyclingValue(options: List<String>, current: String?, fallback: String?): String {
        val normalizedCurrent = normalizeText(current)
        if (normalizedCurrent in options) return normalizedCurrent
        val normalizedFallback = normalizeText(fallback)
        if (normalizedFallback in options) return normalizedFallback
        return options.firstOrNull().orEmpty()
    }

    override fun buildScreen(document: ConfigDocument, state: ConfigState): (Screen) -> Screen {
        val builder = YetAnotherConfigLib.createBuilder()
            .title(Component.literal(document.title))

        document.categories.forEach { category ->
            addFlattenedCategories(builder, category, state, null)
        }

        return { parent -> builder.build().generateScreen(parent) }
    }

    private fun addFlattenedCategories(
        builder: YetAnotherConfigLib.Builder,
        category: V2Category,
        state: ConfigState,
        parentPath: String?,
    ) {
        val display = if (parentPath == null) category.title else "$parentPath / ${category.title}"
        builder.category(buildCategory(category, state, display))
        category.children.forEach { child -> addFlattenedCategories(builder, child, state, display) }
    }

    private fun buildCategory(category: V2Category, state: ConfigState, displayTitle: String): ConfigCategory {
        val categoryBuilder = ConfigCategory.createBuilder().name(Component.literal(displayTitle))

        category.fields.forEach { field -> buildOptions(field, state).forEach(categoryBuilder::option) }

        return categoryBuilder.build()
    }

    private fun buildOptions(field: ConfigField<*>, state: ConfigState): List<Option<*>> {
        return when (field) {
            is IntMapField -> buildIntMapFieldOptions(field, state)
            is LongMapField -> buildLongMapFieldOptions(field, state)
            is FloatMapField -> buildFloatMapFieldOptions(field, state)
            is DoubleMapField -> buildDoubleMapFieldOptions(field, state)
            is StringMapField -> buildStringMapFieldOptions(field, state)
            is ChoiceMapField -> buildChoiceMapFieldOptions(field, state)
            is RegistryMapField -> buildRegistryMapFieldOptions(field, state)
            is SpecField -> buildSpecFieldOptions(field, state)
            is SpecListField -> buildSpecListFieldOptions(field, state)
            is SpecMapField -> buildSpecMapFieldOptions(field, state)
            else -> listOf(buildOption(field, state))
        }
    }

    private fun buildOption(field: ConfigField<*>, state: ConfigState): Option<*> {
        return when (field) {
            is BooleanField -> option(field, field.defaultValue, { state.boolean(field.key) }, { state.setBoolean(field.key, it) }) {
                BooleanControllerBuilder.create(it)
            }

            is IntField -> option(field, field.defaultValue, { state.int(field.key) }, { state.setInt(field.key, it) }) {
                IntegerFieldControllerBuilder.create(it)
            }

            is IntSliderField -> option(field, field.defaultValue, { state.int(field.key) }, { state.setInt(field.key, it) }) {
                IntegerSliderControllerBuilder.create(it).range(field.min, field.max).step(field.step)
            }

            is LongField -> option(field, field.defaultValue, { state.long(field.key) }, { state.setLong(field.key, it) }) {
                LongFieldControllerBuilder.create(it)
            }

            is LongSliderField -> option(field, field.defaultValue, { state.long(field.key) }, { state.setLong(field.key, it) }) {
                LongSliderControllerBuilder.create(it).range(field.min, field.max).step(field.step)
            }

            is FloatField -> option(field, field.defaultValue, { state.float(field.key) }, { state.setFloat(field.key, it) }) {
                FloatFieldControllerBuilder.create(it)
            }

            is DoubleField -> option(field, field.defaultValue, { state.double(field.key) }, { state.setDouble(field.key, it) }) {
                DoubleFieldControllerBuilder.create(it)
            }

            is StringField -> option(field, field.defaultValue, { state.string(field.key) }, { state.setString(field.key, it) }) {
                StringControllerBuilder.create(it)
            }

            is IntListField -> typedListOption(
                field = field,
                defaultValue = field.defaultValue,
                getter = { state.intList(field.key) },
                setter = { state.setIntList(field.key, it) },
                initialValue = field.defaultValue.firstOrNull() ?: 0,
            ) { option -> IntegerFieldControllerBuilder.create(option) }

            is LongListField -> typedListOption(
                field = field,
                defaultValue = field.defaultValue,
                getter = { state.longList(field.key) },
                setter = { state.setLongList(field.key, it) },
                initialValue = field.defaultValue.firstOrNull() ?: 0L,
            ) { option -> LongFieldControllerBuilder.create(option) }

            is FloatListField -> typedListOption(
                field = field,
                defaultValue = field.defaultValue,
                getter = { state.floatList(field.key) },
                setter = { state.setFloatList(field.key, it) },
                initialValue = field.defaultValue.firstOrNull() ?: 0f,
            ) { option -> FloatFieldControllerBuilder.create(option) }

            is DoubleListField -> typedListOption(
                field = field,
                defaultValue = field.defaultValue,
                getter = { state.doubleList(field.key) },
                setter = { state.setDoubleList(field.key, it) },
                initialValue = field.defaultValue.firstOrNull() ?: 0.0,
            ) { option -> DoubleFieldControllerBuilder.create(option) }

            is StringListField -> typedListOption(
                field = field,
                defaultValue = field.defaultValue,
                getter = { state.stringList(field.key) },
                setter = { state.setStringList(field.key, it) },
                initialValue = field.defaultValue.firstOrNull().orEmpty(),
            ) { option -> StringControllerBuilder.create(option) }

            is ChoiceField -> {
                val options = normalizedCyclingValues(field.options, field.defaultValue, state.string(field.key))
                val fallback = selectCyclingValue(options, state.string(field.key), field.defaultValue)
                stringOptionWithCyclingFallback(
                    field,
                    defaultValue = fallback,
                    { selectCyclingValue(options, state.string(field.key), fallback) },
                    { selected -> state.setString(field.key, selectCyclingValue(options, selected, fallback)) },
                    options,
                )
            }

            is ChoiceListField -> encodedListOption(
                field = field,
                defaultValue = field.defaultValue,
                getter = { state.choiceList(field.key) },
                setter = { state.setChoiceList(field.key, it) },
                initialValue = field.options.firstOrNull().orEmpty(),
                controllerFactory = { option ->
                    val options = normalizedCyclingValues(field.options, *state.choiceList(field.key).toTypedArray())
                    if (options.isEmpty()) StringControllerBuilder.create(option)
                    else CyclingListControllerBuilder.create(option)
                        .values(options)
                        .formatValue { value -> Component.literal(value) }
                },
            )

            is RegistryField -> {
                val discoveredOptions = ConfigRegistryOptions.optionsFor(
                    field.registryId,
                    candidates = listOf(state.registry(field.key), field.defaultValue)
                )
                val options = normalizedCyclingValues(discoveredOptions, field.defaultValue, state.registry(field.key))
                val fallback = selectCyclingValue(options, state.registry(field.key), field.defaultValue)
                stringOptionWithDropdownFallback(
                    field,
                    defaultValue = fallback,
                    { selectCyclingValue(options, state.registry(field.key), fallback) },
                    { selected -> state.setRegistry(field.key, selectCyclingValue(options, selected, fallback)) },
                    options,
                    allowAnyValue = true,
                )
            }

            is RegistryListField -> {
                encodedListOption(
                    field = field,
                    defaultValue = field.defaultValue,
                    getter = { state.registryList(field.key) },
                    setter = { state.setRegistryList(field.key, it) },
                    initialValue = field.defaultValue.firstOrNull().orEmpty(),
                    controllerFactory = { option ->
                        val discoveredOptions = ConfigRegistryOptions.optionsFor(
                            field.registryId,
                            candidates = state.registryList(field.key) + field.defaultValue
                        )
                        val options = normalizedCyclingValues(discoveredOptions, *state.registryList(field.key).toTypedArray())
                        if (options.isEmpty()) StringControllerBuilder.create(option)
                        else DropdownStringControllerBuilder.create(option)
                            .values(options)
                            .allowAnyValue(true)
                            .allowEmptyValue(false)
                    },
                )
            }

            else -> fallbackStringOption(field, state)
        }
    }

    private fun buildChoiceMapFieldOptions(field: ChoiceMapField, state: ConfigState): List<Option<*>> {
        val current = state.choiceMap(field.key)
        val defaults = field.defaultValue
        val keys = (defaults.keys + current.keys).distinct().sorted()
        if (keys.isEmpty()) return listOf(fallbackStringOption(field, state))

        val valueOptions = keys.map { entryKey ->
            val baseField = StringField(
                key = "${field.key}.$entryKey",
                title = "${field.title}[$entryKey]",
                description = field.description,
                defaultValue = defaults[entryKey].orEmpty(),
            )
            val options = normalizedCyclingValues(
                field.options,
                defaults[entryKey],
                current[entryKey],
            )
            val fallback = selectCyclingValue(options, current[entryKey], defaults[entryKey])
            stringOptionWithCyclingFallback(
                field = baseField,
                defaultValue = fallback,
                getter = {
                    val value = state.choiceMap(field.key)[entryKey]
                    selectCyclingValue(options, value, fallback)
                },
                setter = { value ->
                    val next = state.choiceMap(field.key).toMutableMap()
                    next[entryKey] = selectCyclingValue(options, value, fallback)
                    state.setChoiceMap(field.key, next)
                },
                options = options,
            )
        }

        val defaultChoice = field.options.firstOrNull().orEmpty()
        val keysOption = mapKeysOption(
            title = "${field.title} Keys",
            description = field.description,
            defaultKeys = defaults.keys.toList(),
            getter = { state.choiceMap(field.key).keys.toList() },
            setter = { updatedKeys ->
                val existing = state.choiceMap(field.key)
                val next = updatedKeys.associateWith { key -> existing[key] ?: defaults[key] ?: defaultChoice }
                state.setChoiceMap(field.key, next)
            },
        )

        return listOf(keysOption) + valueOptions
    }

    private fun buildIntMapFieldOptions(field: IntMapField, state: ConfigState): List<Option<*>> {
        val current = state.intMap(field.key)
        val defaults = field.defaultValue
        val keys = (defaults.keys + current.keys).distinct().sorted()
        if (keys.isEmpty()) return listOf(fallbackStringOption(field, state))

        val valueOptions = keys.map { entryKey ->
            val itemField = IntField(
                key = "${field.key}.$entryKey",
                title = "${field.title}[$entryKey]",
                description = field.description,
                defaultValue = defaults[entryKey] ?: 0,
            )
            option(itemField, itemField.defaultValue,
                getter = { state.intMap(field.key)[entryKey] ?: itemField.defaultValue },
                setter = { value ->
                    val next = state.intMap(field.key).toMutableMap()
                    next[entryKey] = value
                    state.setIntMap(field.key, next)
                }
            ) { option -> IntegerFieldControllerBuilder.create(option) }
        }

        val keysOption = mapKeysOption(
            title = "${field.title} Keys",
            description = field.description,
            defaultKeys = defaults.keys.toList(),
            getter = { state.intMap(field.key).keys.toList() },
            setter = { updatedKeys ->
                val existing = state.intMap(field.key)
                val next = updatedKeys.associateWith { key -> existing[key] ?: defaults[key] ?: 0 }
                state.setIntMap(field.key, next)
            },
        )

        return listOf(keysOption) + valueOptions
    }

    private fun buildLongMapFieldOptions(field: LongMapField, state: ConfigState): List<Option<*>> {
        val current = state.longMap(field.key)
        val defaults = field.defaultValue
        val keys = (defaults.keys + current.keys).distinct().sorted()
        if (keys.isEmpty()) return listOf(fallbackStringOption(field, state))

        val valueOptions = keys.map { entryKey ->
            val itemField = LongField(
                key = "${field.key}.$entryKey",
                title = "${field.title}[$entryKey]",
                description = field.description,
                defaultValue = defaults[entryKey] ?: 0L,
            )
            option(itemField, itemField.defaultValue,
                getter = { state.longMap(field.key)[entryKey] ?: itemField.defaultValue },
                setter = { value ->
                    val next = state.longMap(field.key).toMutableMap()
                    next[entryKey] = value
                    state.setLongMap(field.key, next)
                }
            ) { option -> LongFieldControllerBuilder.create(option) }
        }

        val keysOption = mapKeysOption(
            title = "${field.title} Keys",
            description = field.description,
            defaultKeys = defaults.keys.toList(),
            getter = { state.longMap(field.key).keys.toList() },
            setter = { updatedKeys ->
                val existing = state.longMap(field.key)
                val next = updatedKeys.associateWith { key -> existing[key] ?: defaults[key] ?: 0L }
                state.setLongMap(field.key, next)
            },
        )

        return listOf(keysOption) + valueOptions
    }

    private fun buildFloatMapFieldOptions(field: FloatMapField, state: ConfigState): List<Option<*>> {
        val current = state.floatMap(field.key)
        val defaults = field.defaultValue
        val keys = (defaults.keys + current.keys).distinct().sorted()
        if (keys.isEmpty()) return listOf(fallbackStringOption(field, state))

        val valueOptions = keys.map { entryKey ->
            val itemField = FloatField(
                key = "${field.key}.$entryKey",
                title = "${field.title}[$entryKey]",
                description = field.description,
                defaultValue = defaults[entryKey] ?: 0f,
            )
            option(itemField, itemField.defaultValue,
                getter = { state.floatMap(field.key)[entryKey] ?: itemField.defaultValue },
                setter = { value ->
                    val next = state.floatMap(field.key).toMutableMap()
                    next[entryKey] = value
                    state.setFloatMap(field.key, next)
                }
            ) { option -> FloatFieldControllerBuilder.create(option) }
        }

        val keysOption = mapKeysOption(
            title = "${field.title} Keys",
            description = field.description,
            defaultKeys = defaults.keys.toList(),
            getter = { state.floatMap(field.key).keys.toList() },
            setter = { updatedKeys ->
                val existing = state.floatMap(field.key)
                val next = updatedKeys.associateWith { key -> existing[key] ?: defaults[key] ?: 0f }
                state.setFloatMap(field.key, next)
            },
        )

        return listOf(keysOption) + valueOptions
    }

    private fun buildDoubleMapFieldOptions(field: DoubleMapField, state: ConfigState): List<Option<*>> {
        val current = state.doubleMap(field.key)
        val defaults = field.defaultValue
        val keys = (defaults.keys + current.keys).distinct().sorted()
        if (keys.isEmpty()) return listOf(fallbackStringOption(field, state))

        val valueOptions = keys.map { entryKey ->
            val itemField = DoubleField(
                key = "${field.key}.$entryKey",
                title = "${field.title}[$entryKey]",
                description = field.description,
                defaultValue = defaults[entryKey] ?: 0.0,
            )
            option(itemField, itemField.defaultValue,
                getter = { state.doubleMap(field.key)[entryKey] ?: itemField.defaultValue },
                setter = { value ->
                    val next = state.doubleMap(field.key).toMutableMap()
                    next[entryKey] = value
                    state.setDoubleMap(field.key, next)
                }
            ) { option -> DoubleFieldControllerBuilder.create(option) }
        }

        val keysOption = mapKeysOption(
            title = "${field.title} Keys",
            description = field.description,
            defaultKeys = defaults.keys.toList(),
            getter = { state.doubleMap(field.key).keys.toList() },
            setter = { updatedKeys ->
                val existing = state.doubleMap(field.key)
                val next = updatedKeys.associateWith { key -> existing[key] ?: defaults[key] ?: 0.0 }
                state.setDoubleMap(field.key, next)
            },
        )

        return listOf(keysOption) + valueOptions
    }

    private fun buildStringMapFieldOptions(field: StringMapField, state: ConfigState): List<Option<*>> {
        val current = state.stringMap(field.key)
        val defaults = field.defaultValue
        val keys = (defaults.keys + current.keys).distinct().sorted()
        if (keys.isEmpty()) return listOf(fallbackStringOption(field, state))

        val valueOptions = keys.map { entryKey ->
            val itemField = StringField(
                key = "${field.key}.$entryKey",
                title = "${field.title}[$entryKey]",
                description = field.description,
                defaultValue = defaults[entryKey].orEmpty(),
            )
            option(itemField, itemField.defaultValue,
                getter = { state.stringMap(field.key)[entryKey] ?: itemField.defaultValue },
                setter = { value ->
                    val next = state.stringMap(field.key).toMutableMap()
                    next[entryKey] = value
                    state.setStringMap(field.key, next)
                }
            ) { option -> StringControllerBuilder.create(option) }
        }

        val keysOption = mapKeysOption(
            title = "${field.title} Keys",
            description = field.description,
            defaultKeys = defaults.keys.toList(),
            getter = { state.stringMap(field.key).keys.toList() },
            setter = { updatedKeys ->
                val existing = state.stringMap(field.key)
                val next = updatedKeys.associateWith { key -> existing[key] ?: defaults[key].orEmpty() }
                state.setStringMap(field.key, next)
            },
        )

        return listOf(keysOption) + valueOptions
    }

    private fun buildRegistryMapFieldOptions(field: RegistryMapField, state: ConfigState): List<Option<*>> {
        val current = state.registryMap(field.key)
        val defaults = field.defaultValue
        val keys = (defaults.keys + current.keys).distinct().sorted()
        if (keys.isEmpty()) return listOf(fallbackStringOption(field, state))

        val valueOptions = keys.map { entryKey ->
            val baseField = StringField(
                key = "${field.key}.$entryKey",
                title = "${field.title}[$entryKey]",
                description = field.description,
                defaultValue = defaults[entryKey].orEmpty(),
            )
            val discoveredOptions = ConfigRegistryOptions.optionsFor(
                field.registryId,
                candidates = listOfNotNull(current[entryKey], defaults[entryKey]),
            )
            val options = normalizedCyclingValues(
                discoveredOptions,
                defaults[entryKey],
                current[entryKey],
            )
            val fallback = selectCyclingValue(options, current[entryKey], defaults[entryKey])
            stringOptionWithDropdownFallback(
                field = baseField,
                defaultValue = fallback,
                getter = {
                    val value = state.registryMap(field.key)[entryKey]
                    selectCyclingValue(options, value, fallback)
                },
                setter = { value ->
                    val next = state.registryMap(field.key).toMutableMap()
                    next[entryKey] = selectCyclingValue(options, value, fallback)
                    state.setRegistryMap(field.key, next)
                },
                options = options,
                allowAnyValue = true,
            )
        }

        val keysOption = mapKeysOption(
            title = "${field.title} Keys",
            description = field.description,
            defaultKeys = defaults.keys.toList(),
            getter = { state.registryMap(field.key).keys.toList() },
            setter = { updatedKeys ->
                val existing = state.registryMap(field.key)
                val next = updatedKeys.associateWith { key -> existing[key] ?: defaults[key].orEmpty() }
                state.setRegistryMap(field.key, next)
            },
        )

        return listOf(keysOption) + valueOptions
    }

    private fun mapKeysOption(
        title: String,
        description: String?,
        defaultKeys: List<String>,
        getter: () -> List<String>,
        setter: (List<String>) -> Unit,
    ): ListOption<String> {
        val descriptor = StringListField(
            key = title,
            title = title,
            description = description,
            defaultValue = defaultKeys.distinct().sorted(),
        )
        return typedListOption(
            field = descriptor,
            defaultValue = descriptor.defaultValue,
            getter = { getter().distinct().sorted() },
            setter = { keys -> setter(keys.map(String::trim).filter(String::isNotEmpty).distinct()) },
            initialValue = "",
        ) { option -> StringControllerBuilder.create(option) }
    }

    private fun buildSpecFieldOptions(field: SpecField, state: ConfigState): List<Option<*>> {
        val keys = (field.defaultValue.keys + state.spec(field.key).keys).distinct().sorted()
        if (keys.isEmpty()) return listOf(fallbackStringOption(field, state))

        return keys.map { entryKey ->
            val title = "${field.title} / $entryKey"
            val defaultValue = field.defaultValue[entryKey].orEmpty()
            option(
                field = StringField(key = "${field.key}.$entryKey", title = title, description = field.description, defaultValue = defaultValue),
                defaultValue = defaultValue,
                getter = {
                    val current = state.spec(field.key)
                    current[entryKey] ?: field.defaultValue[entryKey].orEmpty()
                },
                setter = { value ->
                    val next = state.spec(field.key).toMutableMap()
                    next[entryKey] = value
                    state.setSpec(field.key, next)
                },
            ) { option -> StringControllerBuilder.create(option) }
        }
    }

    private fun buildSpecListFieldOptions(field: SpecListField, state: ConfigState): List<Option<*>> {
        val currentList = state.specList(field.key)
        val defaultList = field.defaultValue
        val size = maxOf(currentList.size, defaultList.size)
        if (size == 0) return listOf(fallbackStringOption(field, state))

        val options = mutableListOf<Option<*>>()
        for (index in 0 until size) {
            val currentMap = currentList.getOrNull(index).orEmpty()
            val defaultMap = defaultList.getOrNull(index).orEmpty()
            val keys = (defaultMap.keys + currentMap.keys).distinct().sorted()
            if (keys.isEmpty()) continue

            keys.forEach { entryKey ->
                val title = "${field.title}[$index] / $entryKey"
                val defaultValue = defaultMap[entryKey].orEmpty()
                options += option(
                    field = StringField(
                        key = "${field.key}.$index.$entryKey",
                        title = title,
                        description = field.description,
                        defaultValue = defaultValue,
                    ),
                    defaultValue = defaultValue,
                    getter = {
                        state.specList(field.key).getOrNull(index)?.get(entryKey)
                            ?: defaultList.getOrNull(index)?.get(entryKey).orEmpty()
                    },
                    setter = { value ->
                        val next = state.specList(field.key).toMutableList()
                        while (next.size <= index) {
                            next += mapOf()
                        }
                        val nextMap = next[index].toMutableMap()
                        nextMap[entryKey] = value
                        next[index] = nextMap
                        state.setSpecList(field.key, next)
                    },
                ) { option -> StringControllerBuilder.create(option) }
            }
        }

        return options.ifEmpty { listOf(fallbackStringOption(field, state)) }
    }

    private fun buildSpecMapFieldOptions(field: SpecMapField, state: ConfigState): List<Option<*>> {
        val currentOuter = state.specMap(field.key)
        val defaultOuter = field.defaultValue
        val outerKeys = (defaultOuter.keys + currentOuter.keys).distinct().sorted()
        if (outerKeys.isEmpty()) return listOf(fallbackStringOption(field, state))

        val options = mutableListOf<Option<*>>()
        outerKeys.forEach { outerKey ->
            val currentInner = currentOuter[outerKey].orEmpty()
            val defaultInner = defaultOuter[outerKey].orEmpty()
            val innerKeys = (defaultInner.keys + currentInner.keys).distinct().sorted()
            if (innerKeys.isEmpty()) return@forEach

            innerKeys.forEach { innerKey ->
                val title = "${field.title}[$outerKey] / $innerKey"
                val defaultValue = defaultInner[innerKey].orEmpty()
                options += option(
                    field = StringField(
                        key = "${field.key}.$outerKey.$innerKey",
                        title = title,
                        description = field.description,
                        defaultValue = defaultValue,
                    ),
                    defaultValue = defaultValue,
                    getter = {
                        state.specMap(field.key)[outerKey]?.get(innerKey)
                            ?: field.defaultValue[outerKey]?.get(innerKey).orEmpty()
                    },
                    setter = { value ->
                        val nextOuter = state.specMap(field.key).toMutableMap()
                        val nextInner = nextOuter[outerKey].orEmpty().toMutableMap()
                        nextInner[innerKey] = value
                        nextOuter[outerKey] = nextInner
                        state.setSpecMap(field.key, nextOuter)
                    },
                ) { option -> StringControllerBuilder.create(option) }
            }
        }

        return options.ifEmpty { listOf(fallbackStringOption(field, state)) }
    }

    private fun encodedListOption(
        field: ConfigField<*>,
        defaultValue: List<String>,
        getter: () -> List<String>,
        setter: (List<String>) -> Unit,
        initialValue: String = "",
        controllerFactory: (Option<String>) -> ControllerBuilder<String> = { option -> StringControllerBuilder.create(option) },
    ): ListOption<String> {
        val listBuilder = ListOption.createBuilder<String>()
            .name(Component.literal(field.title))
            .binding(defaultValue, Supplier(getter), setter)
            .initial(Supplier { initialValue })
            .controller(controllerFactory)

        field.description?.let { description ->
            listBuilder.description(OptionDescription.of(Component.literal(description)))
        }

        return listBuilder.build()
    }

    private fun <T> typedListOption(
        field: ConfigField<*>,
        defaultValue: List<T>,
        getter: () -> List<T>,
        setter: (List<T>) -> Unit,
        initialValue: T,
        controllerFactory: (Option<T>) -> ControllerBuilder<T>,
    ): ListOption<T> {
        val listBuilder = ListOption.createBuilder<T>()
            .name(Component.literal(field.title))
            .binding(defaultValue, Supplier(getter), setter)
            .initial(Supplier { initialValue })
            .controller(controllerFactory)

        field.description?.let { description ->
            listBuilder.description(OptionDescription.of(Component.literal(description)))
        }

        return listBuilder.build()
    }

    private fun encodedMapOption(
        field: ConfigField<*>,
        defaultValue: Map<String, String>,
        getter: () -> Map<String, String>,
        setter: (Map<String, String>) -> Unit,
    ): ListOption<String> {
        fun encodeRows(value: Map<String, String>): List<String> = value.entries.map { (key, entryValue) -> "$key=$entryValue" }

        fun decodeRows(rows: List<String>): Map<String, String> {
            return rows
                .mapNotNull { row ->
                    val splitAt = row.indexOf('=')
                    if (splitAt < 0) return@mapNotNull null
                    val key = row.substring(0, splitAt).trim()
                    if (key.isEmpty()) return@mapNotNull null
                    key to row.substring(splitAt + 1)
                }
                .toMap()
        }

        val listBuilder = ListOption.createBuilder<String>()
            .name(Component.literal(field.title))
            .binding(encodeRows(defaultValue), Supplier { encodeRows(getter()) }, { rows -> setter(decodeRows(rows)) })
            .initial(Supplier { "key=value" })
            .controller { option -> StringControllerBuilder.create(option) }

        field.description?.let { description ->
            listBuilder.description(OptionDescription.of(Component.literal(description)))
        }

        return listBuilder.build()
    }

    @Suppress("UNCHECKED_CAST")
    private fun decodeStringList(field: ConfigField<*>, encoded: String): List<String>? {
        return ConfigValueCodec.decode(field, encoded) as? List<String>
    }

    @Suppress("UNCHECKED_CAST")
    private fun decodeStringMap(field: ConfigField<*>, encoded: String): Map<String, String>? {
        return ConfigValueCodec.decode(field, encoded) as? Map<String, String>
    }

    private fun <T> option(
        field: ConfigField<*>,
        defaultValue: T,
        getter: () -> T,
        setter: (T) -> Unit,
        controllerFactory: (Option<T>) -> ControllerBuilder<T>,
    ): Option<T> {
        val optionBuilder = Option.createBuilder<T>()
            .name(Component.literal(field.title))
            .binding(Binding.generic(defaultValue, Supplier(getter), setter))

        field.description?.let { description ->
            optionBuilder.description(OptionDescription.of(Component.literal(description)))
        }

        optionBuilder.controller(controllerFactory)
        return optionBuilder.build()
    }

    private fun stringOptionWithDropdownFallback(
        field: ConfigField<*>,
        defaultValue: String,
        getter: () -> String,
        setter: (String) -> Unit,
        options: List<String>,
        allowAnyValue: Boolean,
    ): Option<String> {
        if (options.isEmpty()) {
            return option(field, defaultValue, getter, setter) { StringControllerBuilder.create(it) }
        }

        return try {
            option(field, defaultValue, getter, setter) {
                DropdownStringControllerBuilder.create(it)
                    .values(options)
                    .allowAnyValue(allowAnyValue)
                    .allowEmptyValue(false)
            }
        } catch (t: Throwable) {
            Archie.LOGGER.warn(
                "Falling back to string controller for config field '{}' due to dropdown controller init error: {}",
                field.key,
                t.message,
            )
            option(field, defaultValue, getter, setter) { StringControllerBuilder.create(it) }
        }
    }

    private fun stringOptionWithCyclingFallback(
        field: ConfigField<*>,
        defaultValue: String,
        getter: () -> String,
        setter: (String) -> Unit,
        options: List<String>,
    ): Option<String> {
        if (options.isEmpty()) {
            return option(field, defaultValue, getter, setter) { StringControllerBuilder.create(it) }
        }

        return try {
            option(field, defaultValue, getter, setter) {
                CyclingListControllerBuilder.create(it)
                    .values(options)
                    .formatValue { value -> Component.literal(value) }
            }
        } catch (t: Throwable) {
            Archie.LOGGER.warn(
                "Falling back to string controller for config field '{}' due to cycling controller init error: {}",
                field.key,
                t.message,
            )
            option(field, defaultValue, getter, setter) { StringControllerBuilder.create(it) }
        }
    }

    private fun fallbackStringOption(field: ConfigField<*>, state: ConfigState): Option<String> {
        val defaultEncoded = ConfigValueCodec.encode(field, field.defaultValue as Any)

        return option(
            field = field,
            defaultValue = defaultEncoded,
            getter = {
                val current = state.allValues()[field.key] ?: field.defaultValue
                ConfigValueCodec.encode(field, current as Any)
            },
            setter = { encoded ->
                val decoded = ConfigValueCodec.decode(field, encoded) ?: return@option
                state.setValue(field.key, decoded)
            },
            controllerFactory = { option -> StringControllerBuilder.create(option) }
        )
    }

}
