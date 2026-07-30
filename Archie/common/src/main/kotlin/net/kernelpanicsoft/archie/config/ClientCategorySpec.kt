package net.kernelpanicsoft.archie.config

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry
import me.shedaniel.clothconfig2.api.ConfigCategory
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.gui.entries.SubCategoryListEntry
import me.shedaniel.math.Color
import net.kernelpanicsoft.archie.config.builder.*
import net.kernelpanicsoft.archie.util.toMutableEntry
import net.minecraft.core.Registry
import net.minecraft.network.chat.Component
import java.util.function.Consumer
import java.util.function.Supplier
import kotlin.reflect.KClass

/**
 * Client-side mirror of a [CategorySpec], built lazily as [CategorySpec.client]. Every `boolean`/
 * `int`/... method here is called by its [CategorySpec] counterpart (via [CategorySpec.onClient])
 * with matching parameters, and queues a [ConfigEntryBuilder]-based entry that reads from and
 * writes back into the same backing maps on [spec]. [buildRoot]/[buildSub] then turn the queued
 * entries into an actual Cloth Config [ConfigCategory]/[SubCategoryListEntry]. None of this is
 * called directly by mod authors - see [CategorySpec] for the public DSL.
 */
@Suppress("unused")
class ClientCategorySpec(internal val spec: CategorySpec)
{
	/** Queued entry builders, appended to in declaration order by each field-registering method below. */
	internal val builders: MutableList<ConfigEntryBuilder.() -> AbstractConfigListEntry<*>> = mutableListOf()

	/** Builds this category's entries plus its subcategories (as nested [SubCategoryListEntry]s) directly into the top-level [category]. */
	internal fun buildRoot(category: ConfigCategory, entryBuilder: ConfigEntryBuilder)
	{
		builders.forEach { builder ->
			category.addEntry(entryBuilder.builder())
		}
		spec.subcategories.forEach { subcategory ->
			category.addEntry(ClientCategorySpec(subcategory).buildSub(entryBuilder))
		}
	}

	/** Builds this category (and its subcategories, recursively) as a single [SubCategoryListEntry]. */
	internal fun buildSub(entryBuilder: ConfigEntryBuilder): SubCategoryListEntry
	{
		val category = entryBuilder.startSubCategory(spec.title)

		builders.forEach { builder ->
			category.add(entryBuilder.builder())
		}

		spec.subcategories.forEach { subcategory ->
			category.add(ClientCategorySpec(subcategory).buildSub(entryBuilder))
		}

		return category.build()
	}

	/** Queues a read-only description entry showing [text], used to render a field's `comment` above it. */
	internal fun comment(text: Component)
	{
		builders.add { startTextDescription(text).build() }
	}

	internal fun boolean(
		id: String,
		title: Component,
		comment: Component? = null,
		default: Boolean = false,
		resetKey: Component? = null
	)
	{
		if (comment != null)
		{
			comment(comment)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			spec.booleans.getOrPut(id) { default }
			val ret = startBooleanToggle(title, spec.booleans.getOrPut(id) { default })
				.apply {
					saveConsumer = Consumer {
						spec.booleans[id] = it
					}
					defaultValue = Supplier {
						default
					}
				}
				.build()
			resetButtonKey = reset
			ret
		}
	}


	internal fun int(
		id: String,
		title: Component,
		comment: Component? = null,
		default: Int = 0,
		resetKey: Component? = null
	)
	{
		if (comment != null)
		{
			comment(comment)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			spec.ints.getOrPut(id) { default }
			val ret = startIntField(title, spec.ints.getOrPut(id) { default })
				.apply {
					saveConsumer = Consumer {
						spec.ints[id] = it
					}
					defaultValue = Supplier {
						default
					}
				}
				.build()
			resetButtonKey = reset
			ret
		}


	}

	internal fun long(
		id: String,
		title: Component,
		comment: Component? = null,
		default: Long = 0,
		resetKey: Component? = null
	)
	{
		if (comment != null)
		{
			comment(comment)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			spec.longs.getOrPut(id) { default }
			val ret = startLongField(title, spec.longs.getOrPut(id) { default })
				.apply {
					saveConsumer = Consumer {
						spec.longs[id] = it
					}
					defaultValue = Supplier {
						default
					}
				}
				.build()
			resetButtonKey = reset
			ret
		}


	}

	internal fun intSlider(
		id: String,
		title: Component,
		comment: Component? = null,
		min: Int,
		max: Int,
		default: Int = min + (max - min) / 2,
		resetKey: Component? = null
	)
	{
		if (comment != null)
		{
			comment(comment)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			spec.ints.getOrPut(id) { default }
			val ret = startIntSlider(title, spec.ints.getOrPut(id) { default }, min, max)
				.apply {
					saveConsumer = Consumer {
						spec.ints[id] = it
					}
					defaultValue = Supplier {
						default
					}
				}
				.build()
			resetButtonKey = reset
			ret
		}


	}

	internal fun longSlider(
		id: String,
		title: Component,
		comment: Component? = null,
		min: Long,
		max: Long,
		default: Long = min + (max - min) / 2,
		resetKey: Component? = null
	)
	{
		if (comment != null)
		{
			comment(comment)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			spec.longs.getOrPut(id) { default }
			val ret = startLongSlider(title, spec.longs.getOrPut(id) { default }, min, max)
				.apply {
					saveConsumer = Consumer {
						spec.longs[id] = it
					}
					defaultValue = Supplier {
						default
					}
				}
				.build()
			resetButtonKey = reset
			ret
		}


	}

	internal fun float(
		id: String,
		title: Component,
		comment: Component? = null,
		default: Float = 0.0f,
		resetKey: Component? = null
	)
	{
		if (comment != null)
		{
			comment(comment)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			spec.floats.getOrPut(id) { default }
			val ret = startFloatField(title, spec.floats.getOrPut(id) { default })
				.apply {
					saveConsumer = Consumer {
						spec.floats[id] = it
					}
					defaultValue = Supplier {
						default
					}
				}
				.build()
			resetButtonKey = reset
			ret
		}


	}

	internal fun double(
		id: String,
		title: Component,
		comment: Component? = null,
		default: Double = 0.0,
		resetKey: Component? = null
	)
	{
		if (comment != null)
		{
			comment(comment)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			spec.doubles.getOrPut(id) { default }
			val ret = startDoubleField(title, spec.doubles.getOrPut(id) { default })
				.apply {
					saveConsumer = Consumer {
						spec.doubles[id] = it
					}
					defaultValue = Supplier {
						default
					}
				}
				.build()
			resetButtonKey = reset
			ret
		}


	}

	internal fun string(
		id: String,
		title: Component,
		comment: Component? = null,
		default: String = "",
		resetKey: Component? = null
	)
	{
		if (comment != null)
		{
			comment(comment)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			spec.strings.getOrPut(id) { default }
			val ret = startStrField(title, spec.strings.getOrPut(id) { default })
				.apply {
					saveConsumer = Consumer {
						spec.strings[id] = it
					}
					defaultValue = Supplier {
						default
					}
				}
				.build()
			resetButtonKey = reset
			ret
		}


	}

	@Suppress("UNCHECKED_CAST")
	internal fun <T : CategorySpec> spec(
		id: String,
		title: Component,
		comment: Component? = null,
		default: T,
		resetKey: Component? = null
	)
	{
		if (comment != null)
		{
			comment(comment)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			(spec.specs as MutableMap<String, T>).getOrPut(id) { default }
			val ret = startSpecField(
				title,
				spec.specs.getOrPut(id) { default })
				.apply {
					saveConsumer = Consumer {
						spec.specs[id] = it
					}
					defaultValue = Supplier {
						default
					}
				}
				.build()
			resetButtonKey = reset
			ret
		}


	}

	internal fun <T : Any, R : T> registry(
		id: String,
		title: Component,
		comment: Component? = null,
		default: T,
		registry: Registry<T>,
		resetKey: Component? = null,
		subclass: KClass<R>? = null,
	)
	{
		if (comment != null)
		{
			comment(comment)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			val ret = startRegistryField(
				title,
				registry.get(spec.registries.getOrPut(id) {
					registry.getKey(
						default
					)!!
				}) ?: default,
				subclass,
				registry
			)
				.apply {
					setSaveConsumer {
						spec.registries[id] = registry.getKey(it)!!
					}
					defaultValue = Supplier {
						default
					}
				}
				.build()
			resetButtonKey = reset
			ret
		}
	}

	internal fun keycode(
		id: String,
		title: Component,
		comment: Component? = null,
		default: CommonKeyCode = CommonKeyCode.unknown,
		resetKey: Component? = null
	)
	{
		if (comment != null)
		{
			comment(comment)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			val ret = startModifierKeyCodeField(
				title,
				spec.keycodes.getOrPut(id) { default }.toClient()
			)
				.apply {
					setModifierSaveConsumer {
						spec.keycodes[id] = it.toCommon()
					}
					setModifierDefaultValue {
						default.toClient()
					}
				}
				.build()
			resetButtonKey = reset
			ret
		}
	}

	internal fun color(
		id: String,
		title: Component,
		comment: Component? = null,
		default: Color = Color.ofTransparent(-1),
		alpha: Boolean = false,
		resetKey: Component? = null
	)
	{
		if (comment != null)
		{
			comment(comment)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			val ret = startColorField(
				title,
				spec.colors.getOrPut(id) { default }.color
			)
				.apply {
					setSaveConsumer2 {
						spec.colors[id] = it
					}
					setDefaultValue2 {
						default
					}
					alphaMode = alpha
				}
				.build()
			resetButtonKey = reset
			ret
		}
	}

	@Suppress("UNCHECKED_CAST")
	internal fun <T : Enum<T>> enumSelector(
		id: String,
		title: Component,
		comment: Component? = null,
		kclass: KClass<T>,
		default: T,
		resetKey: Component? = null
	)
	{
		if (comment != null)
		{
			comment(comment)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			spec.enums.getOrPut(id) { default }
			val ret = startEnumSelector(title, kclass.java, spec.enums.getOrPut(id) { default } as T)
				.apply {
					saveConsumer = Consumer {
						spec.enums[id] = it
					}
					defaultValue = Supplier {
						default
					}
				}
				.build()
			resetButtonKey = reset
			ret
		}
	}

	@Suppress("UNCHECKED_CAST")
	internal fun <T : Any> selector(
		id: String,
		title: Component,
		comment: Component? = null,
		kclass: KClass<T>,
		default: T,
		entries: Array<T>,
		resetKey: Component? = null
	)
	{
		if (comment != null)
		{
			comment(comment)
		}

		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			spec.selectors.getOrPut(id) { default }
			val ret = startSelector(title, entries, spec.selectors.getOrPut(id) { default } as T)
				.apply {
					saveConsumer = Consumer {
						spec.selectors[id] = it
					}
					defaultValue = Supplier {
						default
					}
				}
				.build()
			resetButtonKey = reset
			ret
		}
	}

	internal fun intList(
		id: String,
		title: Component,
		comment: Component? = null,
		default: List<Int> = listOf(),
		resetKey: Component? = null
	)
	{
		if (comment != null)
		{
			comment(comment)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			spec.intLists.getOrPut(id) { default }
			val ret = startIntList(
				title,
				spec.intLists.getOrPut(id) { default })
				.apply {
					saveConsumer = Consumer {
						spec.intLists[id] = it
					}
					defaultValue = Supplier {
						default
					}
				}
				.build()
			resetButtonKey = reset
			ret
		}
	}

	internal fun longList(
		id: String,
		title: Component,
		comment: Component? = null,
		default: List<Long> = listOf(),
		resetKey: Component? = null
	)
	{
		if (comment != null)
		{
			comment(comment)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			spec.longLists.getOrPut(id) { default }
			val ret = startLongList(
				title,
				spec.longLists.getOrPut(id) { default })
				.apply {
					saveConsumer = Consumer {
						spec.longLists[id] = it
					}
					defaultValue = Supplier {
						default
					}
				}
				.build()
			resetButtonKey = reset
			ret
		}
	}

	internal fun floatList(
		id: String,
		title: Component,
		comment: Component? = null,
		default: List<Float> = listOf(),
		resetKey: Component? = null
	)
	{
		if (comment != null)
		{
			comment(comment)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			spec.floatLists.getOrPut(id) { default }
			val ret = startFloatList(
				title,
				spec.floatLists.getOrPut(id) { default })
				.apply {
					saveConsumer = Consumer {
						spec.floatLists[id] = it
					}
					defaultValue = Supplier {
						default
					}
				}
				.build()
			resetButtonKey = reset
			ret
		}
	}

	internal fun doubleList(
		id: String,
		title: Component,
		comment: Component? = null,
		default: List<Double> = listOf(),
		resetKey: Component? = null
	)
	{
		if (comment != null)
		{
			comment(comment)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			spec.doubleLists.getOrPut(id) { default }
			val ret = startDoubleList(
				title,
				spec.doubleLists.getOrPut(id) { default })
				.apply {
					saveConsumer = Consumer {
						spec.doubleLists[id] = it
					}
					defaultValue = Supplier {
						default
					}
				}
				.build()
			resetButtonKey = reset
			ret
		}
	}

	internal fun stringList(
		id: String,
		title: Component,
		comment: Component? = null,
		default: List<String> = listOf(),
		resetKey: Component? = null
	)
	{
		if (comment != null)
		{
			comment(comment)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			spec.stringLists.getOrPut(id) { default }
			val ret = startStrList(
				title,
				spec.stringLists.getOrPut(id) { default })
				.apply {
					saveConsumer = Consumer {
						spec.stringLists[id] = it
					}
					defaultValue = Supplier {
						default
					}
				}
				.build()
			resetButtonKey = reset
			ret
		}
	}

	@Suppress("UNCHECKED_CAST")
	internal fun <T : CategorySpec> specList(
		id: String,
		title: Component,
		comment: Component? = null,
		default: List<T> = listOf(),
		factory: () -> T,
		resetKey: Component? = null
	)
	{
		if (comment != null)
		{
			comment(comment)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			(spec.specLists as MutableMap<String, List<T>>).getOrPut(id) { default }
			val ret = startSpecList(
				title,
				spec.specLists.getOrPut(
					id
				) { default },
				factory
			)
				.apply {
					saveConsumer = Consumer {
						spec.specLists[id] = it as List<T>
					}
					defaultValue = Supplier {
						default
					}
				}
				.build()
			resetButtonKey = reset
			ret
		}
	}

	internal fun <T : Any, R : T> registryList(
		id: String,
		title: Component,
		comment: Component? = null,
		default: List<T> = listOf(),
		factory: () -> T,
		registry: Registry<T>,
		resetKey: Component? = null,
		subclass: KClass<R>? = null
	)
	{
		if (comment != null)
		{
			comment(comment)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			val ret = startRegistryList(
				title,
				spec.registryLists.getOrPut(
					id
				) { default.map { registry.getKey(it)!! } }
					.map { registry.get(it) ?: factory() },
				factory,
				subclass,
				registry
			)
				.apply {
					setSaveConsumer { value ->
						spec.registryLists[id] = value.map { registry.getKey(it)!! }
					}

					setDefaultValue {
						default
					}
				}
				.build()
			resetButtonKey = reset
			ret
		}
	}

	internal fun keycodeList(
		id: String,
		title: Component,
		comment: Component? = null,
		default: List<CommonKeyCode> = listOf(),
		factory: () -> CommonKeyCode = CommonKeyCode.Companion::unknown,
		resetKey: Component? = null
	)
	{
		if (comment != null)
		{
			comment(comment)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			spec.keycodeLists.getOrPut(id) { default }
			val ret = startKeycodeList(
				title,
				spec.keycodeLists.getOrPut(
					id
				) { default }.map { it.toClient() }
			) { factory().toClient() }
				.apply {
					saveConsumer = Consumer { value ->
						spec.keycodeLists[id] = value.map { it.toCommon() }
					}
					defaultValue = Supplier {
						default.map { it.toClient() }
					}
				}
				.build()
			resetButtonKey = reset
			ret
		}
	}

	internal fun colorList(
		id: String,
		title: Component,
		comment: Component? = null,
		default: List<Color> = listOf(),
		factory: () -> Color = { Color.ofTransparent(-1) },
		resetKey: Component? = null
	)
	{
		if (comment != null)
		{
			comment(comment)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			val ret = startColorList(
				title,
				spec.colorLists.getOrPut(id) { default },
				factory
			)
				.apply {
					setSaveConsumer {
						spec.colorLists[id] = if (alphaMode)
							it.map(Color::ofTransparent)
						else
							it.map(Color::ofOpaque)
					}
					setDefaultValue {
						default.map { it.color }
					}
				}
				.build()
			resetButtonKey = reset
			ret
		}
	}

	internal fun intMap(
		id: String,
		title: Component,
		comment: Component? = null,
		default: Map<String, Int> = mapOf(),
		resetKey: Component? = null
	)
	{
		if (comment != null)
		{
			comment(comment)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			spec.intMaps.getOrPut(id) { default }
			val ret = startIntMap(
				title,
				spec.intMaps.getOrPut(id) { default })
				.apply {
					saveConsumer = Consumer { value ->
						spec.intMaps[id] = value.associate { it.toPair() }
					}
					defaultValue = Supplier {
						default.entries.toList().map { it.toMutableEntry() }
					}
				}
				.build()
			resetButtonKey = reset
			ret
		}
	}

	internal fun longMap(
		id: String,
		title: Component,
		comment: Component? = null,
		default: Map<String, Long> = mapOf(),
		resetKey: Component? = null
	)
	{
		if (comment != null)
		{
			comment(comment)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			spec.longMaps.getOrPut(id) { default }
			val ret = startLongMap(
				title,
				spec.longMaps.getOrPut(id) { default })
				.apply {
					saveConsumer = Consumer { value ->
						spec.longMaps[id] = value.associate { it.toPair() }
					}
					defaultValue = Supplier {
						default.entries.toList().map { it.toMutableEntry() }
					}
				}
				.build()
			resetButtonKey = reset
			ret
		}
	}

	internal fun floatMap(
		id: String,
		title: Component,
		comment: Component? = null,
		default: Map<String, Float> = mapOf(),
		resetKey: Component? = null
	)
	{
		if (comment != null)
		{
			comment(comment)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			spec.floatMaps.getOrPut(id) { default }
			val ret = startFloatMap(
				title,
				spec.floatMaps.getOrPut(id) { default })
				.apply {
					saveConsumer = Consumer { value ->
						spec.floatMaps[id] = value.associate { it.toPair() }
					}
					defaultValue = Supplier {
						default.entries.toList().map { it.toMutableEntry() }
					}
				}
				.build()
			resetButtonKey = reset
			ret
		}
	}

	internal fun doubleMap(
		id: String,
		title: Component,
		comment: Component? = null,
		default: Map<String, Double> = mapOf(),
		resetKey: Component? = null
	)
	{
		if (comment != null)
		{
			comment(comment)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			spec.doubleMaps.getOrPut(id) { default }
			val ret = startDoubleMap(
				title,
				spec.doubleMaps.getOrPut(id) { default })
				.apply {
					saveConsumer = Consumer { value ->
						spec.doubleMaps[id] = value.associate { it.toPair() }
					}
					defaultValue = Supplier {
						default.entries.toList().map { it.toMutableEntry() }
					}
				}
				.build()
			resetButtonKey = reset
			ret
		}
	}

	internal fun stringMap(
		id: String,
		title: Component,
		comment: Component? = null,
		default: Map<String, String> = mapOf(),
		resetKey: Component? = null
	)
	{
		if (comment != null)
		{
			comment(comment)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			spec.stringMaps.getOrPut(id) { default }
			val ret = startStrMap(
				title,
				spec.stringMaps.getOrPut(id) { default })
				.apply {
					saveConsumer = Consumer { value ->
						spec.stringMaps[id] = value.associate { it.toPair() }
					}
					defaultValue = Supplier {
						default.entries.toList().map { it.toMutableEntry() }
					}
				}
				.build()
			resetButtonKey = reset
			ret
		}
	}

	@Suppress("UNCHECKED_CAST")
	internal fun <T : CategorySpec> specMap(
		id: String,
		title: Component,
		comment: Component? = null,
		default: Map<String, T> = mapOf(),
		factory: () -> T,
		resetKey: Component? = null
	)
	{
		if (comment != null)
		{
			comment(comment)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			(spec.specMaps as MutableMap<String, Map<String, T>>).getOrPut(id) { default }
			val ret = startSpecMap(
				title,
				spec.specMaps.getOrPut(
					id
				) { default },
				factory
			)
				.apply {
					saveConsumer = Consumer { value ->
						spec.specMaps[id] = value.associate { it.toPair() }
					}
					defaultValue = Supplier {
						default.entries.toList().map { it.toMutableEntry() }
					}
				}
				.build()
			resetButtonKey = reset
			ret
		}
	}

	internal fun <T : Any, R : T> registryMap(
		id: String,
		title: Component,
		comment: Component? = null,
		default: Map<String, T> = mapOf(),
		factory: () -> T,
		registry: Registry<T>,
		resetKey: Component? = null,
		subclass: KClass<R>? = null
	)
	{
		if (comment != null)
		{
			comment(comment)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			val ret = startRegistryMap(
				title,
				spec.registryMaps.getOrPut(
					id
				) { default.mapValues { registry.getKey(it.value)!! } }
					.mapValues {
						registry.get(it.value) ?: factory()
					},
				factory,
				subclass,
				registry
			)
				.apply {
					setSaveConsumer { value ->
						spec.registryMaps[id] =
							value.associate { it.toPair() }.mapValues { registry.getKey(it.value)!! }
					}

					setDefaultValue {
						default.toList().map { it.toMutableEntry() }
					}
				}
				.build()
			resetButtonKey = reset
			ret
		}
	}

	internal fun keycodeMap(
		id: String,
		title: Component,
		comment: Component? = null,
		default: Map<String, CommonKeyCode> = mapOf(),
		factory: () -> CommonKeyCode = CommonKeyCode.Companion::unknown,
		resetKey: Component? = null
	)
	{
		if (comment != null)
		{
			comment(comment)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			spec.keycodeMaps.getOrPut(id) { default }
			val ret = startKeycodeMap(
				title,
				spec.keycodeMaps.getOrPut(id) { default }.mapValues { it.value.toClient() }
			) { factory().toClient() }
				.apply {
					saveConsumer = Consumer { value ->
						spec.keycodeMaps[id] =
							value.associate { it.toPair() }.mapValues { it.value.toCommon() }
					}
					defaultValue = Supplier {
						default.mapValues { it.value.toClient() }.entries.toList().map { it.toMutableEntry() }
					}
				}
				.build()
			resetButtonKey = reset
			ret
		}
	}

	internal fun colorMap(
		id: String,
		title: Component,
		comment: Component? = null,
		default: Map<String, Color> = mapOf(),
		factory: () -> Color = { Color.ofTransparent(-1) },
		resetKey: Component? = null
	)
	{
		if (comment != null)
		{
			comment(comment)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			val ret = startColorMap(
				title,
				spec.colorMaps.getOrPut(
					id
				) { default },
				factory
			)
				.apply {
					setSaveConsumer { value ->
						spec.colorMaps[id] = if (alphaMode)
							value.associate { it.toPair() }.mapValues { Color.ofTransparent(it.value) }
						else
							value.associate { it.toPair() }.mapValues { Color.ofOpaque(it.value) }
					}
					setDefaultValue {
						default.mapValues { it.value.color }.toList().map { it.toMutableEntry() }
					}
				}
				.build()
			resetButtonKey = reset
			ret
		}
	}
}

