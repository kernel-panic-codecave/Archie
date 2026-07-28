package net.kernelpanicsoft.archie.config

import dev.architectury.platform.Platform
import dev.architectury.utils.Env
import io.github.xn32.json5k.SerialComment
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import me.shedaniel.math.Color
import net.minecraft.core.Registry
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.peanuuutz.tomlkt.TomlComment
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass

@Suppress("unused")
abstract class CategorySpec(val title: Component, val id: String = title.string.toSnakeCase())
{
	val client by lazy { ClientCategorySpec(this) }

	inline fun onClient(block: () -> Unit) = if (Platform.getEnvironment() == Env.CLIENT) block() else Unit

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
	internal val keycodes: MutableMap<String, CommonKeyCode> = mutableMapOf()
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
	internal val keycodeLists: MutableMap<String, List<CommonKeyCode>> = mutableMapOf()
	internal val colorLists: MutableMap<String, List<Color>> = mutableMapOf()
	internal val intMaps: MutableMap<String, Map<String, Int>> = mutableMapOf()
	internal val longMaps: MutableMap<String, Map<String, Long>> = mutableMapOf()
	internal val floatMaps: MutableMap<String, Map<String, Float>> = mutableMapOf()
	internal val doubleMaps: MutableMap<String, Map<String, Double>> = mutableMapOf()
	internal val stringMaps: MutableMap<String, Map<String, String>> = mutableMapOf()
	internal val specMaps: MutableMap<String, Map<String, CategorySpec>> = mutableMapOf()
	internal val registryMaps: MutableMap<String, Map<String, ResourceLocation>> = mutableMapOf()
	internal val keycodeMaps: MutableMap<String, Map<String, CommonKeyCode>> = mutableMapOf()
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

	protected fun boolean(
		title: Component,
		comment: Component? = null,
		default: Boolean = false,
		resetKey: Component? = null
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Boolean>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.string
			}
			onClient {
				client.boolean(id, title, comment, default, resetKey)
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
		comment: Component? = null,
		default: Int = 0,
		resetKey: Component? = null
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Int>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.string
			}
			onClient {
				client.int(id, title, comment, default, resetKey)
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
		comment: Component? = null,
		default: Long = 0,
		resetKey: Component? = null
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Long>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.string
			}
			onClient {
				client.long(id, title, comment, default, resetKey)
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
		comment: Component? = null,
		min: Int,
		max: Int,
		default: Int = min + (max - min) / 2,
		resetKey: Component? = null
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Int>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.string
			}
			onClient {
				client.intSlider(id, title, comment, min, max, default, resetKey)
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
		comment: Component? = null,
		min: Long,
		max: Long,
		default: Long = min + (max - min) / 2,
		resetKey: Component? = null
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Long>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.string
			}
			onClient {
				client.longSlider(id, title, comment, min, max, default, resetKey)
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
		comment: Component? = null,
		default: Float = 0.0f,
		resetKey: Component? = null
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Float>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.string
			}
			onClient {
				client.float(id, title, comment, default, resetKey)
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
		comment: Component? = null,
		default: Double = 0.0,
		resetKey: Component? = null
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Double>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.string
			}
			onClient {
				client.double(id, title, comment, default, resetKey)
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
		comment: Component? = null,
		default: String = "",
		resetKey: Component? = null
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, String>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.string
			}
			onClient {
				client.string(id, title, comment, default, resetKey)
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
		comment: Component? = null,
		default: T,
		resetKey: Component? = null
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, T>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.string
			}
			onClient {
				client.spec(id, title, comment, default, resetKey)
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
		comment: Component? = null,
		default: T,
		registry: Registry<T>,
		resetKey: Component? = null,
		subclass: KClass<R>? = null,
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, R>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.string
			}
			onClient {
				client.registry(id, title, comment, default, registry, resetKey, subclass)
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
		comment: Component? = null,
		default: CommonKeyCode = CommonKeyCode.unknown,
		resetKey: Component? = null
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, CommonKeyCode>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.string
			}
			onClient {
				client.keycode(id, title, comment, default, resetKey)
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
		comment: Component? = null,
		default: Color = Color.ofTransparent(-1),
		alpha: Boolean = false,
		resetKey: Component? = null
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Color>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.string
			}
			onClient {
				client.color(id, title, comment, default, alpha, resetKey)
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
		comment: Component? = null,
		kclass: KClass<T>,
		default: T,
		resetKey: Component? = null
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, T>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.string
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
		comment: Component? = null,
		kclass: KClass<T>,
		default: T,
		entries: Array<T>,
		resetKey: Component? = null
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, T>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.string
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
		comment: Component? = null,
		default: List<Int> = listOf(),
		resetKey: Component? = null
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, List<Int>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.string
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
		comment: Component? = null,
		default: List<Long> = listOf(),
		resetKey: Component? = null
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, List<Long>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.string
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
		comment: Component? = null,
		default: List<Float> = listOf(),
		resetKey: Component? = null
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, List<Float>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.string
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
		comment: Component? = null,
		default: List<Double> = listOf(),
		resetKey: Component? = null
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, List<Double>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.string
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
		comment: Component? = null,
		default: List<String> = listOf(),
		resetKey: Component? = null
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, List<String>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.string
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
		comment: Component? = null,
		default: List<T> = listOf(),
		factory: () -> T,
		resetKey: Component? = null
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, List<T>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.string
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
		comment: Component? = null,
		default: List<T> = listOf(),
		factory: () -> T,
		registry: Registry<T>,
		resetKey: Component? = null,
		subclass: KClass<R>? = null
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, List<R>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.string
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
		comment: Component? = null,
		default: List<CommonKeyCode> = listOf(),
		factory: () -> CommonKeyCode = CommonKeyCode.Companion::unknown,
		resetKey: Component? = null
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, List<CommonKeyCode>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.string
			}
			types[id] = FieldType.KeyCodeList
			keycodeLists.putIfAbsent(id, default )
			ReadOnlyProperty { _, _ ->
				keycodeLists.getOrPut(id) {
					default
				}
			}
		}

	protected fun colorList(
		title: Component,
		comment: Component? = null,
		default: List<Color> = listOf(),
		factory: () -> Color = { Color.ofTransparent(-1) },
		resetKey: Component? = null
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, List<Color>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.string
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
		comment: Component? = null,
		default: Map<String, Int> = mapOf(),
		resetKey: Component? = null
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Map<String, Int>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.string
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
		comment: Component? = null,
		default: Map<String, Long> = mapOf(),
		resetKey: Component? = null
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Map<String, Long>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.string
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
		comment: Component? = null,
		default: Map<String, Float> = mapOf(),
		resetKey: Component? = null
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Map<String, Float>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.string
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
		comment: Component? = null,
		default: Map<String, Double> = mapOf(),
		resetKey: Component? = null
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Map<String, Double>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.string
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
		comment: Component? = null,
		default: Map<String, String> = mapOf(),
		resetKey: Component? = null
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Map<String, String>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.string
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
		comment: Component? = null,
		default: Map<String, T> = mapOf(),
		factory: () -> T,
		resetKey: Component? = null
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Map<String, T>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.string
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
		comment: Component? = null,
		default: Map<String, T> = mapOf(),
		factory: () -> T,
		registry: Registry<T>,
		resetKey: Component? = null,
		subclass: KClass<R>? = null
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Map<String, R>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.string
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
		comment: Component? = null,
		default: Map<String, CommonKeyCode> = mapOf(),
		factory: () -> CommonKeyCode = CommonKeyCode.Companion::unknown,
		resetKey: Component? = null
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Map<String, CommonKeyCode>>> =
		PropertyDelegateProvider { _, property ->
			val id = property.name.toSnakeCase()
			if (comment != null)
			{
				comments[id] = comment.string
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
		comment: Component? = null,
		default: Map<String, Color> = mapOf(),
		factory: () -> Color = { Color.ofTransparent(-1) },
		resetKey: Component? = null
	): PropertyDelegateProvider<CategorySpec, ReadOnlyProperty<CategorySpec, Map<String, Color>>> =
	PropertyDelegateProvider { _, property ->
		val id = property.name.toSnakeCase()
		if (comment != null)
		{
			comments[id] = comment.string
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
										decodeSerializableElement(descriptor, index, type.serializer)

									is FieldType.Selector -> selectors[key] =
										decodeSerializableElement(descriptor, index, type.serializer)

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

