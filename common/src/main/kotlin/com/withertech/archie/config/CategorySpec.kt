package com.withertech.archie.config

import com.withertech.archie.config.builder.*
import com.withertech.archie.util.MutableEntry
import com.withertech.archie.util.toMutableEntry
import io.github.xn32.json5k.SerialComment
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry
import me.shedaniel.clothconfig2.api.ConfigCategory
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.api.ModifierKeyCode
import me.shedaniel.clothconfig2.gui.entries.SubCategoryListEntry
import me.shedaniel.clothconfig2.impl.builders.*
import me.shedaniel.math.Color
import net.minecraft.core.Registry
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.animal.Cat
import net.peanuuutz.tomlkt.TomlComment
import java.util.function.Consumer
import java.util.function.Supplier
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass

@Suppress("unused")
abstract class CategorySpec(internal val title: Component, internal val id: String = title.string.toSnakeCase())
{
	internal val builders: MutableList<ConfigEntryBuilder.() -> AbstractConfigListEntry<*>> = mutableListOf()
	internal val types: MutableMap<String, FieldType<*>> = linkedMapOf()
	internal val comments: MutableMap<String, String> = mutableMapOf()
	internal val booleans: MutableMap<String, Boolean> = mutableMapOf()
	internal val ints: MutableMap<String, Int> = mutableMapOf()
	internal val longs: MutableMap<String, Long> = mutableMapOf()
	internal val floats: MutableMap<String, Float> = mutableMapOf()
	internal val doubles: MutableMap<String, Double> = mutableMapOf()
	internal val strings: MutableMap<String, String> = mutableMapOf()
	internal val specs: MutableMap<String, CategorySpec> = mutableMapOf()
	internal val registries: MutableMap<String, ResourceLocation> = mutableMapOf()
	internal val keycodes: MutableMap<String, ModifierKeyCode> = mutableMapOf()
	internal val colors: MutableMap<String, Color> = mutableMapOf()
	internal val enums: MutableMap<String, Enum<*>> = mutableMapOf()
	internal val selectors: MutableMap<String, Any> = mutableMapOf()
	internal val intLists: MutableMap<String, List<Int>> = mutableMapOf()
	internal val longLists: MutableMap<String, List<Long>> = mutableMapOf()
	internal val floatLists: MutableMap<String, List<Float>> = mutableMapOf()
	internal val doubleLists: MutableMap<String, List<Double>> = mutableMapOf()
	internal val stringLists: MutableMap<String, List<String>> = mutableMapOf()
	internal val specLists: MutableMap<String, List<CategorySpec>> = mutableMapOf()
	internal val registryLists: MutableMap<String, List<ResourceLocation>> = mutableMapOf()
	internal val keycodeLists: MutableMap<String, List<ModifierKeyCode>> = mutableMapOf()
	internal val colorLists: MutableMap<String, List<Color>> = mutableMapOf()
	internal val intMaps: MutableMap<String, Map<String, Int>> = mutableMapOf()
	internal val longMaps: MutableMap<String, Map<String, Long>> = mutableMapOf()
	internal val floatMaps: MutableMap<String, Map<String, Float>> = mutableMapOf()
	internal val doubleMaps: MutableMap<String, Map<String, Double>> = mutableMapOf()
	internal val stringMaps: MutableMap<String, Map<String, String>> = mutableMapOf()
	internal val specMaps: MutableMap<String, Map<String, CategorySpec>> = mutableMapOf()
	internal val registryMaps: MutableMap<String, Map<String, ResourceLocation>> = mutableMapOf()
	internal val keycodeMaps: MutableMap<String, Map<String, ModifierKeyCode>> = mutableMapOf()
	internal val colorMaps: MutableMap<String, Map<String, Color>> = mutableMapOf()

	open val subcategories: List<CategorySpec> = listOf()

	open val isEnabled: Boolean = true

	internal fun init()
	{
		subcategories.forEach { cat ->
			types[cat.id] = FieldType.Category(cat)
			if (cat.subcategories.isNotEmpty())
				cat.init()
		}
	}

	internal fun buildRoot(category: ConfigCategory, entryBuilder: ConfigEntryBuilder)
	{
		builders.forEach { builder ->
			category.addEntry(entryBuilder.builder())
		}
		subcategories.forEach { subcategory ->
			category.addEntry(subcategory.buildSub(entryBuilder))
		}
	}

	internal fun buildSub(entryBuilder: ConfigEntryBuilder): SubCategoryListEntry
	{
		val category = entryBuilder.startSubCategory(title)

		builders.forEach { builder ->
			category.add(entryBuilder.builder())
		}

		subcategories.forEach { subcategory ->
			category.add(subcategory.buildSub(entryBuilder))
		}

		return category.build()
	}

	protected fun comment(
		text: Component,
		block: TextDescriptionBuilder.() -> Unit = {}
	): Comment
	{
		return text.string to {
			startTextDescription(text)
				.apply(block)
				.build()
		}
	}

	protected fun boolean(
		title: Component,
		comment: Comment? = null,
		default: Boolean = false,
		resetKey: Component? = null,
		block: BooleanToggleBuilder.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Boolean>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.first
				builders.add(comment.second)
			}
			builders.add {
				val reset = resetButtonKey
				resetButtonKey = resetKey ?: resetButtonKey
				booleans.getOrPut(id) { default }
				val ret = startBooleanToggle(title, booleans.getOrPut(id) { default })
					.apply {
						saveConsumer = Consumer {
							booleans[id] = it
						}
						defaultValue = Supplier {
							default
						}
					}
					.apply(
						block
					)
					.build()
				resetButtonKey = reset
				ret
			}
			types[id] = FieldType.Boolean
			booleans.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				booleans.getOrPut(id) {
					default
				}
			}
		}

	protected fun int(
		title: Component,
		comment: Comment? = null,
		default: Int = 0,
		resetKey: Component? = null,
		block: IntFieldBuilder.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Int>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.first
				builders.add(comment.second)
			}
			builders.add {
				val reset = resetButtonKey
				resetButtonKey = resetKey ?: resetButtonKey
				ints.getOrPut(id) { default }
				val ret = startIntField(title, ints.getOrPut(id) { default })
					.apply {
						saveConsumer = Consumer {
							ints[id] = it
						}
						defaultValue = Supplier {
							default
						}
					}
					.apply(
						block
					)
					.build()
				resetButtonKey = reset
				ret
			}
			types[id] = FieldType.Int
			ints.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				ints.getOrPut(id) {
					default
				}
			}
		}

	protected fun long(
		title: Component,
		comment: Comment? = null,
		default: Long = 0,
		resetKey: Component? = null,
		block: LongFieldBuilder.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Long>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.first
				builders.add(comment.second)
			}
			builders.add {
				val reset = resetButtonKey
				resetButtonKey = resetKey ?: resetButtonKey
				longs.getOrPut(id) { default }
				val ret = startLongField(title, longs.getOrPut(id) { default })
					.apply {
						saveConsumer = Consumer {
							longs[id] = it
						}
						defaultValue = Supplier {
							default
						}
					}
					.apply(
						block
					)
					.build()
				resetButtonKey = reset
				ret
			}
			types[id] = FieldType.Long
			longs.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				longs.getOrPut(id) {
					default
				}
			}
		}

	protected fun intSlider(
		title: Component,
		comment: Comment? = null,
		min: Int,
		max: Int,
		default: Int = min + (max - min) / 2,
		resetKey: Component? = null,
		block: IntSliderBuilder.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Int>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.first
				builders.add(comment.second)
			}
			builders.add {
				val reset = resetButtonKey
				resetButtonKey = resetKey ?: resetButtonKey
				ints.getOrPut(id) { default }
				val ret = startIntSlider(title, ints.getOrPut(id) { default }, min, max)
					.apply {
						saveConsumer = Consumer {
							ints[id] = it
						}
						defaultValue = Supplier {
							default
						}
					}
					.apply(
						block
					)
					.build()
				resetButtonKey = reset
				ret
			}
			types[id] = FieldType.Int
			ints.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				ints.getOrPut(id) {
					default
				}
			}
		}

	protected fun longSlider(
		title: Component,
		comment: Comment? = null,
		min: Long,
		max: Long,
		default: Long = min + (max - min) / 2,
		resetKey: Component? = null,
		block: LongSliderBuilder.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Long>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.first
				builders.add(comment.second)
			}
			builders.add {
				val reset = resetButtonKey
				resetButtonKey = resetKey ?: resetButtonKey
				longs.getOrPut(id) { default }
				val ret = startLongSlider(title, longs.getOrPut(id) { default }, min, max)
					.apply {
						saveConsumer = Consumer {
							longs[id] = it
						}
						defaultValue = Supplier {
							default
						}
					}
					.apply(
						block
					)
					.build()
				resetButtonKey = reset
				ret
			}
			types[id] = FieldType.Long
			longs.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				longs.getOrPut(id) {
					default
				}
			}
		}

	protected fun float(
		title: Component,
		comment: Comment? = null,
		default: Float = 0.0f,
		resetKey: Component? = null,
		block: FloatFieldBuilder.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Float>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.first
				builders.add(comment.second)
			}
			builders.add {
				val reset = resetButtonKey
				resetButtonKey = resetKey ?: resetButtonKey
				floats.getOrPut(id) { default }
				val ret = startFloatField(title, floats.getOrPut(id) { default })
					.apply {
						saveConsumer = Consumer {
							floats[id] = it
						}
						defaultValue = Supplier {
							default
						}
					}
					.apply(
						block
					)
					.build()
				resetButtonKey = reset
				ret
			}
			types[id] = FieldType.Float
			floats.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				floats.getOrPut(id) {
					default
				}
			}
		}

	protected fun double(
		title: Component,
		comment: Comment? = null,
		default: Double = 0.0,
		resetKey: Component? = null,
		block: DoubleFieldBuilder.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Double>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.first
				builders.add(comment.second)
			}
			builders.add {
				val reset = resetButtonKey
				resetButtonKey = resetKey ?: resetButtonKey
				doubles.getOrPut(id) { default }
				val ret = startDoubleField(title, doubles.getOrPut(id) { default })
					.apply {
						saveConsumer = Consumer {
							doubles[id] = it
						}
						defaultValue = Supplier {
							default
						}
					}
					.apply(
						block
					)
					.build()
				resetButtonKey = reset
				ret
			}
			types[id] = FieldType.Double
			doubles.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				doubles.getOrPut(id) {
					default
				}
			}
		}

	protected fun string(
		title: Component,
		comment: Comment? = null,
		default: String = "",
		resetKey: Component? = null,
		block: StringFieldBuilder.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, String>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.first
				builders.add(comment.second)
			}
			builders.add {
				val reset = resetButtonKey
				resetButtonKey = resetKey ?: resetButtonKey
				strings.getOrPut(id) { default }
				val ret = startStrField(title, strings.getOrPut(id) { default })
					.apply {
						saveConsumer = Consumer {
							strings[id] = it
						}
						defaultValue = Supplier {
							default
						}
					}
					.apply(
						block
					)
					.build()
				resetButtonKey = reset
				ret
			}
			types[id] = FieldType.String
			strings.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				strings.getOrPut(id) {
					default
				}
			}
		}

	@Suppress("UNCHECKED_CAST")
	protected fun <T : CategorySpec> spec(
		title: Component,
		comment: Comment? = null,
		default: T,
		resetKey: Component? = null,
		block: SpecFieldBuilder<T>.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, T>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.first
				builders.add(comment.second)
			}
			builders.add {
				val reset = resetButtonKey
				resetButtonKey = resetKey ?: resetButtonKey
				(specs as MutableMap<String, T>).getOrPut(id) { default }
				val ret = startSpecField(
					title,
					specs.getOrPut(id) { default })
					.apply {
						saveConsumer = Consumer {
							specs[id] = it
						}
						defaultValue = Supplier {
							default
						}
					}
					.apply(
						block
					)
					.build()
				resetButtonKey = reset
				ret
			}
			types[id] = FieldType.Spec { default } as FieldType<T>
			(specs as MutableMap<String, T>).putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				(specs as MutableMap<String, T>).getOrPut(id) {
					default
				}
			}
		}

	protected fun <T : Any, R : T> registry(
		title: Component,
		comment: Comment? = null,
		default: T,
		registry: Registry<T>,
		resetKey: Component? = null,
		subclass: KClass<R>? = null,
		block: RegistryFieldBuilder<T, R>.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, R>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.first
				builders.add(comment.second)
			}
			builders.add {
				val reset = resetButtonKey
				resetButtonKey = resetKey ?: resetButtonKey
				val ret = startRegistryField(
					title,
					registry.get(registries.getOrPut(id) {
						registry.getKey(
							default
						)!!
					}) ?: default,
					subclass,
					registry
				)
					.apply {
						setSaveConsumer {
							registries[id] = registry.getKey(it)!!
						}
						defaultValue = Supplier {
							default
						}
					}
					.apply(
						block
					)
					.build()
				resetButtonKey = reset
				ret
			}
			types[id] = FieldType.Registry
			registries.putIfAbsent(id, registry.getKey(default)!!)
			ReadOnlyProperty { _, _ ->
				@Suppress("UNCHECKED_CAST")
				registry.get(registries.getOrPut(id) {
					registry.getKey(default)!!
				})!! as R
			}
		}

	protected fun keycode(
		title: Component,
		comment: Comment? = null,
		default: ModifierKeyCode = ModifierKeyCode.unknown(),
		resetKey: Component? = null,
		block: KeyCodeBuilder.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, ModifierKeyCode>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.first
				builders.add(comment.second)
			}
			builders.add {
				val reset = resetButtonKey
				resetButtonKey = resetKey ?: resetButtonKey
				val ret = startModifierKeyCodeField(
					title,
					keycodes.getOrPut(id) { default })
					.apply {
						setModifierSaveConsumer {
							keycodes[id] = it
						}
						setModifierDefaultValue {
							default
						}
					}
					.apply(
						block
					)
					.build()
				resetButtonKey = reset
				ret
			}
			types[id] = FieldType.KeyCode
			keycodes.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				keycodes.getOrPut(id) {
					default
				}
			}
		}

	protected fun color(
		title: Component,
		comment: Comment? = null,
		default: Color = Color.ofTransparent(-1),
		resetKey: Component? = null,
		block: ColorFieldBuilder.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Color>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.first
				builders.add(comment.second)
			}
			builders.add {
				val reset = resetButtonKey
				resetButtonKey = resetKey ?: resetButtonKey
				val ret = startColorField(
					title,
					colors.getOrPut(id) { default }.color
				)
					.apply {
						setSaveConsumer2 {
							colors[id] = it
						}
						setDefaultValue2 {
							default
						}
					}
					.apply(
						block
					)
					.build()
				resetButtonKey = reset
				ret
			}
			types[id] = FieldType.Color
			colors.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				colors.getOrPut(id) {
					default
				}
			}
		}

	@Suppress("UNCHECKED_CAST")
	protected fun <T : Enum<T>> enumSelector(
		title: Component,
		comment: Comment? = null,
		kclass: KClass<T>,
		default: T,
		resetKey: Component? = null,
		block: EnumSelectorBuilder<T>.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, T>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.first
				builders.add(comment.second)
			}
			builders.add {
				val reset = resetButtonKey
				resetButtonKey = resetKey ?: resetButtonKey
				enums.getOrPut(id) { default }
				val ret = startEnumSelector(title, kclass.java, enums.getOrPut(id) { default } as T)
					.apply {
						saveConsumer = Consumer {
							enums[id] = it
						}
						defaultValue = Supplier {
							default
						}
					}
					.apply(
						block
					)
					.build()
				resetButtonKey = reset
				ret
			}
			types[id] = FieldType.EnumSelector(kclass)
			enums.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				enums.getOrPut(id) {
					default
				} as T
			}
		}

	@Suppress("UNCHECKED_CAST")
	protected fun <T : Any> selector(
		title: Component,
		comment: Comment? = null,
		kclass: KClass<T>,
		default: T,
		entries: Array<T>,
		resetKey: Component? = null,
		block: SelectorBuilder<T>.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, T>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.first
				builders.add(comment.second)
			}

			builders.add {
				val reset = resetButtonKey
				resetButtonKey = resetKey ?: resetButtonKey
				selectors.getOrPut(id) { default }
				val ret = startSelector(title, entries, selectors.getOrPut(id) { default } as T)
					.apply {
						saveConsumer = Consumer {
							selectors[id] = it
						}
						defaultValue = Supplier {
							default
						}
					}
					.apply(
						block
					)
					.build()
				resetButtonKey = reset
				ret
			}
			types[id] = FieldType.Selector(kclass)
			selectors.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				selectors.getOrPut(id) {
					default
				} as T
			}
		}

	protected fun intList(
		title: Component,
		comment: Comment? = null,
		default: List<Int> = listOf(),
		resetKey: Component? = null,
		block: IntListBuilder.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, List<Int>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.first
				builders.add(comment.second)
			}
			builders.add {
				val reset = resetButtonKey
				resetButtonKey = resetKey ?: resetButtonKey
				intLists.getOrPut(id) { default }
				val ret = startIntList(
					title,
					intLists.getOrPut(id) { default })
					.apply {
						saveConsumer = Consumer {
							intLists[id] = it
						}
						defaultValue = Supplier {
							default
						}
					}
					.apply(
						block
					)
					.build()
				resetButtonKey = reset
				ret
			}
			types[id] = FieldType.IntList
			intLists.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				intLists.getOrPut(id) {
					default
				}
			}
		}

	protected fun longList(
		title: Component,
		comment: Comment? = null,
		default: List<Long> = listOf(),
		resetKey: Component? = null,
		block: LongListBuilder.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, List<Long>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.first
				builders.add(comment.second)
			}
			builders.add {
				val reset = resetButtonKey
				resetButtonKey = resetKey ?: resetButtonKey
				longLists.getOrPut(id) { default }
				val ret = startLongList(
					title,
					longLists.getOrPut(id) { default })
					.apply {
						saveConsumer = Consumer {
							longLists[id] = it
						}
						defaultValue = Supplier {
							default
						}
					}
					.apply(
						block
					)
					.build()
				resetButtonKey = reset
				ret
			}
			types[id] = FieldType.LongList
			longLists.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				longLists.getOrPut(id) {
					default
				}
			}
		}

	protected fun floatList(
		title: Component,
		comment: Comment? = null,
		default: List<Float> = listOf(),
		resetKey: Component? = null,
		block: FloatListBuilder.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, List<Float>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.first
				builders.add(comment.second)
			}
			builders.add {
				val reset = resetButtonKey
				resetButtonKey = resetKey ?: resetButtonKey
				floatLists.getOrPut(id) { default }
				val ret = startFloatList(
					title,
					floatLists.getOrPut(id) { default })
					.apply {
						saveConsumer = Consumer {
							floatLists[id] = it
						}
						defaultValue = Supplier {
							default
						}
					}
					.apply(
						block
					)
					.build()
				resetButtonKey = reset
				ret
			}
			types[id] = FieldType.FloatList
			floatLists.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				floatLists.getOrPut(id) {
					default
				}
			}
		}

	protected fun doubleList(
		title: Component,
		comment: Comment? = null,
		default: List<Double> = listOf(),
		resetKey: Component? = null,
		block: DoubleListBuilder.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, List<Double>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.first
				builders.add(comment.second)
			}
			builders.add {
				val reset = resetButtonKey
				resetButtonKey = resetKey ?: resetButtonKey
				doubleLists.getOrPut(id) { default }
				val ret = startDoubleList(
					title,
					doubleLists.getOrPut(id) { default })
					.apply {
						saveConsumer = Consumer {
							doubleLists[id] = it
						}
						defaultValue = Supplier {
							default
						}
					}
					.apply(
						block
					)
					.build()
				resetButtonKey = reset
				ret
			}
			types[id] = FieldType.DoubleList
			doubleLists.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				doubleLists.getOrPut(id) {
					default
				}
			}
		}

	protected fun stringList(
		title: Component,
		comment: Comment? = null,
		default: List<String> = listOf(),
		resetKey: Component? = null,
		block: StringListBuilder.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, List<String>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.first
				builders.add(comment.second)
			}
			builders.add {
				val reset = resetButtonKey
				resetButtonKey = resetKey ?: resetButtonKey
				stringLists.getOrPut(id) { default }
				val ret = startStrList(
					title,
					stringLists.getOrPut(id) { default })
					.apply {
						saveConsumer = Consumer {
							stringLists[id] = it
						}
						defaultValue = Supplier {
							default
						}
					}
					.apply(
						block
					)
					.build()
				resetButtonKey = reset
				ret
			}
			types[id] = FieldType.StringList
			stringLists.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				stringLists.getOrPut(id) {
					default
				}
			}
		}

	@Suppress("UNCHECKED_CAST")
	protected fun <T : CategorySpec> specList(
		title: Component,
		comment: Comment? = null,
		default: List<T> = listOf(),
		factory: () -> T,
		resetKey: Component? = null,
		block: SpecListBuilder<T>.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, List<T>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.first
				builders.add(comment.second)
			}
			builders.add {
				val reset = resetButtonKey
				resetButtonKey = resetKey ?: resetButtonKey
				(specLists as MutableMap<String, List<T>>).getOrPut(id) { default }
				val ret = startSpecList(
					title,
					specLists.getOrPut(
						id
					) { default },
					factory
				)
					.apply {
						saveConsumer = Consumer {
							specLists[id] = it as List<T>
						}
						defaultValue = Supplier {
							default
						}
					}
					.apply(
						block
					)
					.build()
				resetButtonKey = reset
				ret
			}
			types[id] = FieldType.SpecList(factory) as FieldType<List<T>>
			(specLists).putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				(specLists as MutableMap<String, List<T>>).getOrPut(id) {
					default
				}
			}
		}

	protected fun <T : Any, R : T> registryList(
		title: Component,
		comment: Comment? = null,
		default: List<T> = listOf(),
		factory: () -> T,
		registry: Registry<T>,
		resetKey: Component? = null,
		subclass: KClass<R>? = null,
		block: RegistryListBuilder<T, R>.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, List<R>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.first
				builders.add(comment.second)
			}
			builders.add {
				val reset = resetButtonKey
				resetButtonKey = resetKey ?: resetButtonKey
				val ret = startRegistryList(
					title,
					registryLists.getOrPut(
						id
					) { default.map { registry.getKey(it)!! } }
						.map { registry.get(it) ?: factory() },
					factory,
					subclass,
					registry
				)
					.apply {
						setSaveConsumer { value ->
							registryLists[id] = value.map { registry.getKey(it)!! }
						}

						setDefaultValue {
							default
						}
					}
					.apply(
						block
					)
					.build()
				resetButtonKey = reset
				ret
			}
			types[id] = FieldType.RegistryList
			registryLists.putIfAbsent(id, default.map { registry.getKey(it)!! })
			ReadOnlyProperty { _, _ ->
				@Suppress("UNCHECKED_CAST")
				registryLists.getOrPut(id) {
					default.map { registry.getKey(it)!! }
				}.map { registry.get(it)!! } as List<R>
			}
		}

	protected fun keycodeList(
		title: Component,
		comment: Comment? = null,
		default: List<ModifierKeyCode> = listOf(),
		factory: () -> ModifierKeyCode = ModifierKeyCode::unknown,
		resetKey: Component? = null,
		block: KeycodeListBuilder.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, List<ModifierKeyCode>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.first
				builders.add(comment.second)
			}
			builders.add {
				val reset = resetButtonKey
				resetButtonKey = resetKey ?: resetButtonKey
				keycodeLists.getOrPut(id) { default }
				val ret = startKeycodeList(
					title,
					keycodeLists.getOrPut(
						id
					) { default },
					factory
				)
					.apply {
						saveConsumer = Consumer {
							keycodeLists[id] = it
						}
						defaultValue = Supplier {
							default
						}
					}
					.apply(
						block
					)
					.build()
				resetButtonKey = reset
				ret
			}
			types[id] = FieldType.KeyCodeList
			keycodeLists.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				keycodeLists.getOrPut(id) {
					default
				}
			}
		}

	protected fun colorList(
		title: Component,
		comment: Comment? = null,
		default: List<Color> = listOf(),
		factory: () -> Color = { Color.ofTransparent(-1) },
		resetKey: Component? = null,
		block: ColorListBuilder.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, List<Color>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.first
				builders.add(comment.second)
			}
			builders.add {
				val reset = resetButtonKey
				resetButtonKey = resetKey ?: resetButtonKey
				val ret = startColorList(
					title,
					colorLists.getOrPut(id) { default },
					factory
				)
					.apply {
						setSaveConsumer {
							colorLists[id] = if (alphaMode)
								it.map(Color::ofTransparent)
							else
								it.map(Color::ofOpaque)
						}
						setDefaultValue {
							default.map { it.color }
						}
					}
					.apply(
						block
					)
					.build()
				resetButtonKey = reset
				ret
			}
			types[id] = FieldType.ColorList
			colorLists.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				colorLists.getOrPut(id) {
					default
				}
			}
		}

	protected fun intMap(
		title: Component,
		comment: Comment? = null,
		default: Map<String, Int> = mapOf(),
		resetKey: Component? = null,
		block: IntegerMapBuilder.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Map<String, Int>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.first
				builders.add(comment.second)
			}
			builders.add {
				val reset = resetButtonKey
				resetButtonKey = resetKey ?: resetButtonKey
				intMaps.getOrPut(id) { default }
				val ret = startIntMap(
					title,
					intMaps.getOrPut(id) { default })
					.apply {
						saveConsumer = Consumer { value ->
							intMaps[id] = value.associate { it.toPair() }
						}
						defaultValue = Supplier {
							default.entries.toList().map { it.toMutableEntry() }
						}
					}
					.apply(
						block
					)
					.build()
				resetButtonKey = reset
				ret
			}
			types[id] = FieldType.IntMap
			intMaps.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				intMaps.getOrPut(id) {
					default
				}
			}
		}

	protected fun longMap(
		title: Component,
		comment: Comment? = null,
		default: Map<String, Long> = mapOf(),
		resetKey: Component? = null,
		block: LongMapBuilder.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Map<String, Long>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.first
				builders.add(comment.second)
			}
			builders.add {
				val reset = resetButtonKey
				resetButtonKey = resetKey ?: resetButtonKey
				longMaps.getOrPut(id) { default }
				val ret = startLongMap(
					title,
					longMaps.getOrPut(id) { default })
					.apply {
						saveConsumer = Consumer<List<MutableEntry<String, Long>>> { value ->
							longMaps[id] = value.associate { it.toPair() }
						}
						defaultValue = Supplier<List<MutableEntry<String, Long>>> {
							default.entries.toList().map { it.toMutableEntry() }
						}
					}
					.apply(
						block
					)
					.build()
				resetButtonKey = reset
				ret
			}
			types[id] = FieldType.LongMap
			longMaps.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				longMaps.getOrPut(id) {
					default
				}
			}
		}

	protected fun floatMap(
		title: Component,
		comment: Comment? = null,
		default: Map<String, Float> = mapOf(),
		resetKey: Component? = null,
		block: FloatMapBuilder.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Map<String, Float>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.first
				builders.add(comment.second)
			}
			builders.add {
				val reset = resetButtonKey
				resetButtonKey = resetKey ?: resetButtonKey
				floatMaps.getOrPut(id) { default }
				val ret = startFloatMap(
					title,
					floatMaps.getOrPut(id) { default })
					.apply {
						saveConsumer = Consumer { value ->
							floatMaps[id] = value.associate { it.toPair() }
						}
						defaultValue = Supplier {
							default.entries.toList().map { it.toMutableEntry() }
						}
					}
					.apply(
						block
					)
					.build()
				resetButtonKey = reset
				ret
			}
			types[id] = FieldType.FloatMap
			floatMaps.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				floatMaps.getOrPut(id) {
					default
				}
			}
		}

	protected fun doubleMap(
		title: Component,
		comment: Comment? = null,
		default: Map<String, Double> = mapOf(),
		resetKey: Component? = null,
		block: DoubleMapBuilder.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Map<String, Double>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.first
				builders.add(comment.second)
			}
			builders.add {
				val reset = resetButtonKey
				resetButtonKey = resetKey ?: resetButtonKey
				doubleMaps.getOrPut(id) { default }
				val ret = startDoubleMap(
					title,
					doubleMaps.getOrPut(id) { default })
					.apply {
						saveConsumer = Consumer { value ->
							doubleMaps[id] = value.associate { it.toPair() }
						}
						defaultValue = Supplier {
							default.entries.toList().map { it.toMutableEntry() }
						}
					}
					.apply(
						block
					)
					.build()
				resetButtonKey = reset
				ret
			}
			types[id] = FieldType.DoubleMap
			doubleMaps.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				doubleMaps.getOrPut(id) {
					default
				}
			}
		}

	protected fun stringMap(
		title: Component,
		comment: Comment? = null,
		default: Map<String, String> = mapOf(),
		resetKey: Component? = null,
		block: StringMapBuilder.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Map<String, String>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.first
				builders.add(comment.second)
			}
			builders.add {
				val reset = resetButtonKey
				resetButtonKey = resetKey ?: resetButtonKey
				stringMaps.getOrPut(id) { default }
				val ret = startStrMap(
					title,
					stringMaps.getOrPut(id) { default })
					.apply {
						saveConsumer = Consumer<List<MutableEntry<String, String>>> { value ->
							stringMaps[id] = value.associate { it.toPair() }
						}
						defaultValue = Supplier {
							default.entries.toList().map { it.toMutableEntry() }
						}
					}
					.apply(
						block
					)
					.build()
				resetButtonKey = reset
				ret
			}
			types[id] = FieldType.StringMap
			stringMaps.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				stringMaps.getOrPut(id) {
					default
				}
			}
		}

	@Suppress("UNCHECKED_CAST")
	protected fun <T : CategorySpec> specMap(
		title: Component,
		comment: Comment? = null,
		default: Map<String, T> = mapOf(),
		factory: () -> T,
		resetKey: Component? = null,
		block: SpecMapBuilder<T>.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Map<String, T>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.first
				builders.add(comment.second)
			}
			builders.add {
				val reset = resetButtonKey
				resetButtonKey = resetKey ?: resetButtonKey
				(specMaps as MutableMap<String, Map<String, T>>).getOrPut(id) { default }
				val ret = startSpecMap(
					title,
					specMaps.getOrPut(
						id
					) { default },
					factory
				)
					.apply {
						saveConsumer = Consumer { value ->
							specMaps[id] = value.associate { it.toPair() }
						}
						defaultValue = Supplier {
							default.entries.toList().map { it.toMutableEntry() }
						}
					}
					.apply(
						block
					)
					.build()
				resetButtonKey = reset
				ret
			}
			types[id] = FieldType.SpecMap(factory) as FieldType<Map<String, T>>
			(specMaps as MutableMap<String, Map<String, T>>).putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				(specMaps as MutableMap<String, Map<String, T>>).getOrPut(id) {
					default
				}
			}
		}

	protected fun <T : Any, R : T> registryMap(
		title: Component,
		comment: Comment? = null,
		default: Map<String, T> = mapOf(),
		factory: () -> T,
		registry: Registry<T>,
		resetKey: Component? = null,
		subclass: KClass<R>? = null,
		block: RegistryMapBuilder<T, R>.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Map<String, R>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.first
				builders.add(comment.second)
			}
			builders.add {
				val reset = resetButtonKey
				resetButtonKey = resetKey ?: resetButtonKey
				val ret = startRegistryMap(
					title,
					registryMaps.getOrPut(
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
							registryMaps[id] = value.associate { it.toPair() }.mapValues { registry.getKey(it.value)!! }
						}

						setDefaultValue {
							default.toList().map { it.toMutableEntry() }
						}
					}
					.apply(
						block
					)
					.build()
				resetButtonKey = reset
				ret
			}
			types[id] = FieldType.RegistryMap
			registryMaps.putIfAbsent(id, default.mapValues { registry.getKey(it.value)!! })
			ReadOnlyProperty { _, _ ->
				@Suppress("UNCHECKED_CAST")
				registryMaps.getOrPut(id) {
					default.mapValues { registry.getKey(it.value)!! }
				}.mapValues { registry.get(it.value)!! } as Map<String, R>
			}
		}

	protected fun keycodeMap(
		title: Component,
		comment: Comment? = null,
		default: Map<String, ModifierKeyCode> = mapOf(),
		factory: () -> ModifierKeyCode = ModifierKeyCode::unknown,
		resetKey: Component? = null,
		block: KeycodeMapBuilder.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Map<String, ModifierKeyCode>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.first
				builders.add(comment.second)
			}
			builders.add {
				val reset = resetButtonKey
				resetButtonKey = resetKey ?: resetButtonKey
				keycodeMaps.getOrPut(id) { default }
				val ret = startKeycodeMap(
					title,
					keycodeMaps.getOrPut(id) { default },
					factory
				)
					.apply {
						saveConsumer = Consumer { value ->
							keycodeMaps[id] =
								value.associate { it.toPair() }
						}
						defaultValue = Supplier {
							default.entries.toList().map { it.toMutableEntry() }
						}
					}
					.apply(
						block
					)
					.build()
				resetButtonKey = reset
				ret
			}
			types[id] = FieldType.KeyCodeMap
			keycodeMaps.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				keycodeMaps.getOrPut(id) {
					default
				}
			}
		}

	protected fun colorMap(
		title: Component,
		comment: Comment? = null,
		default: Map<String, Color> = mapOf(),
		factory: () -> Color = { Color.ofTransparent(-1) },
		resetKey: Component? = null,
		block: ColorMapBuilder.() -> Unit = {}
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Map<String, Color>>> =
	PropertyDelegateProvider { _, property ->
		val id = property.name.toSnakeCase()
		if (comment != null)
		{
			comments[id] = comment.first
			builders.add(comment.second)
		}
		builders.add {
			val reset = resetButtonKey
			resetButtonKey = resetKey ?: resetButtonKey
			val ret = startColorMap(
				title,
				colorMaps.getOrPut(
					id
				) { default },
				factory
			)
				.apply {
					setSaveConsumer { value ->
						colorMaps[id] = if (alphaMode)
							value.associate { it.toPair() }.mapValues { Color.ofTransparent(it.value) }
						else
							value.associate { it.toPair() }.mapValues { Color.ofOpaque(it.value) }
					}
					setDefaultValue {
						default.mapValues { it.value.color }.toList().map { it.toMutableEntry() }
					}
				}
				.apply(
					block
				)
				.build()
			resetButtonKey = reset
			ret
		}
		types[id] = FieldType.ColorMap
		colorMaps.putIfAbsent(id, default)
		ReadOnlyProperty { _, _ ->
			colorMaps.getOrPut(id) {
				default
			}
		}
	}

	internal class ConfigCategorySerializer(val factory: () -> CategorySpec) :
		KSerializer<CategorySpec>
	{
		override val descriptor: SerialDescriptor by lazy {
			with(factory())
			{
				buildClassSerialDescriptor(title.string)
				{
					types.forEach { (id, type) ->
						element(
							elementName = id,
							descriptor = type.serializer.descriptor,
							annotations = buildList {
								if (id in comments)
								{
									add(TomlComment(comments[id]!!))
									add(SerialComment(comments[id]!!))
								}
							}
						)
					}
				}
			}
		}

		override fun deserialize(decoder: Decoder): CategorySpec
		{
			return decoder.decodeStructure(descriptor)
			{
				val spec = factory()
				with(spec)
				{
					while (true)
					{
						when (val index = decodeElementIndex(descriptor))
						{
							in types.entries.indices ->
							{
								val (key, type) = types.entries.toList()[index]
								when (type)
								{
									is FieldType.Category ->
										decodeSerializableElement(descriptor, index, type.serializer)

									is FieldType.Boolean -> booleans[key] =
										decodeSerializableElement(descriptor, index, type.serializer)

									is FieldType.Int -> ints[key] =
										decodeSerializableElement(descriptor, index, type.serializer)

									is FieldType.Long -> longs[key] =
										decodeSerializableElement(descriptor, index, type.serializer)

									is FieldType.Float -> floats[key] =
										decodeSerializableElement(descriptor, index, type.serializer)

									is FieldType.Double -> doubles[key] =
										decodeSerializableElement(descriptor, index, type.serializer)

									is FieldType.String -> strings[key] =
										decodeSerializableElement(descriptor, index, type.serializer)

									is FieldType.Spec -> specs[key] =
										decodeSerializableElement(descriptor, index, type.serializer)

									is FieldType.Registry -> registries[key] =
										decodeSerializableElement(descriptor, index, type.serializer)

									is FieldType.KeyCode -> keycodes[key] =
										decodeSerializableElement(descriptor, index, type.serializer)

									is FieldType.Color -> colors[key] =
										decodeSerializableElement(descriptor, index, type.serializer)

									is FieldType.EnumSelector -> enums[key] =
										decodeSerializableElement(descriptor, index, type.serializer) as Enum<*>

									is FieldType.Selector -> selectors[key] =
										decodeSerializableElement(descriptor, index, type.serializer) as Any

									is FieldType.IntList -> intLists[key] =
										decodeSerializableElement(descriptor, index, type.serializer)

									is FieldType.LongList -> longLists[key] =
										decodeSerializableElement(descriptor, index, type.serializer)

									is FieldType.FloatList -> floatLists[key] =
										decodeSerializableElement(descriptor, index, type.serializer)

									is FieldType.DoubleList -> doubleLists[key] =
										decodeSerializableElement(descriptor, index, type.serializer)

									is FieldType.StringList -> stringLists[key] =
										decodeSerializableElement(descriptor, index, type.serializer)

									is FieldType.SpecList -> specLists[key] =
										decodeSerializableElement(descriptor, index, type.serializer)

									is FieldType.RegistryList -> registryLists[key] =
										decodeSerializableElement(descriptor, index, type.serializer)

									is FieldType.KeyCodeList -> keycodeLists[key] =
										decodeSerializableElement(descriptor, index, type.serializer)

									is FieldType.ColorList -> colorLists[key] =
										decodeSerializableElement(descriptor, index, type.serializer)

									is FieldType.IntMap -> intMaps[key] =
										decodeSerializableElement(descriptor, index, type.serializer)

									is FieldType.LongMap -> longMaps[key] =
										decodeSerializableElement(descriptor, index, type.serializer)

									is FieldType.FloatMap -> floatMaps[key] =
										decodeSerializableElement(descriptor, index, type.serializer)

									is FieldType.DoubleMap -> doubleMaps[key] =
										decodeSerializableElement(descriptor, index, type.serializer)

									is FieldType.StringMap -> stringMaps[key] =
										decodeSerializableElement(descriptor, index, type.serializer)

									is FieldType.SpecMap -> specMaps[key] =
										decodeSerializableElement(descriptor, index, type.serializer)

									is FieldType.RegistryMap -> registryMaps[key] =
										decodeSerializableElement(descriptor, index, type.serializer)

									is FieldType.KeyCodeMap -> keycodeMaps[key] =
										decodeSerializableElement(descriptor, index, type.serializer)

									is FieldType.ColorMap -> colorMaps[key] =
										decodeSerializableElement(descriptor, index, type.serializer)
								}
							}

							CompositeDecoder.DECODE_DONE -> break
							else -> error("Unexpected index: $index")
						}
					}
				}
				spec
			}
		}

		override fun serialize(encoder: Encoder, value: CategorySpec)
		{
			encoder.encodeStructure(descriptor)
			{
				value.types.entries.forEachIndexed { index, (key, type) ->
					@Suppress("UNCHECKED_CAST")
					when (type)
					{
						is FieldType.Category -> encodeSerializableElement(
							descriptor,
							index,
							type.serializer,
							type.category
						)

						is FieldType.Boolean -> encodeSerializableElement(
							descriptor,
							index,
							type.serializer,
							value.booleans[key]!!
						)

						is FieldType.Int -> encodeSerializableElement(
							descriptor,
							index,
							type.serializer,
							value.ints[key]!!
						)

						is FieldType.Long -> encodeSerializableElement(
							descriptor,
							index,
							type.serializer,
							value.longs[key]!!
						)

						is FieldType.Float -> encodeSerializableElement(
							descriptor,
							index,
							type.serializer,
							value.floats[key]!!
						)

						is FieldType.Double -> encodeSerializableElement(
							descriptor,
							index,
							type.serializer,
							value.doubles[key]!!
						)

						is FieldType.String -> encodeSerializableElement(
							descriptor,
							index,
							type.serializer,
							value.strings[key]!!
						)

						is FieldType.Spec -> encodeSerializableElement(
							descriptor,
							index,
							type.serializer,
							value.specs[key]!!
						)

						is FieldType.Registry -> encodeSerializableElement(
							descriptor,
							index,
							type.serializer,
							value.registries[key]!!
						)

						is FieldType.KeyCode -> encodeSerializableElement(
							descriptor,
							index,
							type.serializer,
							value.keycodes[key]!!
						)

						is FieldType.Color -> encodeSerializableElement(
							descriptor,
							index,
							type.serializer,
							value.colors[key]!!
						)

						is FieldType.EnumSelector -> encodeSerializableElement(
							descriptor,
							index,
							type.serializer as KSerializer<Enum<*>>,
							value.enums[key]!!
						)

						is FieldType.Selector -> encodeSerializableElement(
							descriptor,
							index,
							type.serializer as KSerializer<Any>,
							value.selectors[key]!!
						)

						is FieldType.IntList -> encodeSerializableElement(
							descriptor,
							index,
							type.serializer,
							value.intLists[key]!!
						)

						is FieldType.LongList -> encodeSerializableElement(
							descriptor,
							index,
							type.serializer,
							value.longLists[key]!!
						)

						is FieldType.FloatList -> encodeSerializableElement(
							descriptor,
							index,
							type.serializer,
							value.floatLists[key]!!
						)

						is FieldType.DoubleList -> encodeSerializableElement(
							descriptor,
							index,
							type.serializer,
							value.doubleLists[key]!!
						)

						is FieldType.StringList -> encodeSerializableElement(
							descriptor,
							index,
							type.serializer,
							value.stringLists[key]!!
						)

						is FieldType.SpecList -> encodeSerializableElement(
							descriptor,
							index,
							type.serializer,
							value.specLists[key]!!
						)

						is FieldType.RegistryList -> encodeSerializableElement(
							descriptor,
							index,
							type.serializer,
							value.registryLists[key]!!
						)

						is FieldType.KeyCodeList -> encodeSerializableElement(
							descriptor,
							index,
							type.serializer,
							value.keycodeLists[key]!!
						)

						is FieldType.ColorList -> encodeSerializableElement(
							descriptor,
							index,
							type.serializer,
							value.colorLists[key]!!
						)

						is FieldType.IntMap -> encodeSerializableElement(
							descriptor,
							index,
							type.serializer,
							value.intMaps[key]!!
						)

						is FieldType.LongMap -> encodeSerializableElement(
							descriptor,
							index,
							type.serializer,
							value.longMaps[key]!!
						)

						is FieldType.FloatMap -> encodeSerializableElement(
							descriptor,
							index,
							type.serializer,
							value.floatMaps[key]!!
						)

						is FieldType.DoubleMap -> encodeSerializableElement(
							descriptor,
							index,
							type.serializer,
							value.doubleMaps[key]!!
						)

						is FieldType.StringMap -> encodeSerializableElement(
							descriptor,
							index,
							type.serializer,
							value.stringMaps[key]!!
						)

						is FieldType.SpecMap -> encodeSerializableElement(
							descriptor,
							index,
							type.serializer,
							value.specMaps[key]!!
						)

						is FieldType.RegistryMap -> encodeSerializableElement(
							descriptor,
							index,
							type.serializer,
							value.registryMaps[key]!!
						)

						is FieldType.KeyCodeMap -> encodeSerializableElement(
							descriptor,
							index,
							type.serializer,
							value.keycodeMaps[key]!!
						)

						is FieldType.ColorMap -> encodeSerializableElement(
							descriptor,
							index,
							type.serializer,
							value.colorMaps[key]!!
						)
					}
				}
			}
		}
	}

	internal val serializer by lazy { ConfigCategorySerializer { this } }
}

