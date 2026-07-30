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

/**
 * A section of a [ConfigSpec], declared by subclassing this and adding fields with the
 * `by boolean(...)`, `by int(...)`, etc. delegates below.
 *
 * Each delegate call registers a field under an id derived from the *property* name
 * (snake_cased), stores the field's [FieldType] and default value, and - on the client - mirrors
 * the field into [ClientCategorySpec] so Cloth Config can render it. The delegate itself just
 * reads the current value back out of this category's backing maps, so config values are read
 * with plain property access (e.g. `MyConfig.General.enableFeature`).
 *
 * Nest categories with [subcategories] for grouping in the UI, or use `spec`/`specList`/`specMap`
 * to embed other [CategorySpec] instances as fields.
 *
 * @param title Display title shown in the Cloth Config UI.
 * @param id Stable identifier used as this category's key in its parent and in the serialized
 * file. Defaults to the snake_cased [title].
 */
@Suppress("unused")
abstract class CategorySpec(val title: Component, val id: String = title.string.toSnakeCase())
{
	/** Client-side mirror of this category, used to build the Cloth Config UI. */
	val client by lazy { ClientCategorySpec(this) }

	/** Runs [block] only when called on the client; a no-op on a dedicated server. */
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

	/** Nested categories shown as sub-sections of this one in the UI. Empty by default. */
	open val subcategories: List<CategorySpec> = listOf()

	/**
	 * Whether this category is currently active. When `false`, Cloth Config hides/disables the
	 * category's fields in the UI. Override with a `get()` that reads another field (e.g. a
	 * parent toggle) to make this category conditional.
	 */
	open val isEnabled: Boolean = true

	/** Registers [subcategories] as fields on this category, recursively. */
	internal fun init()
	{
		subcategories.forEach { cat ->
			types[cat.id] = FieldType.Category(cat)
			if (cat.subcategories.isNotEmpty())
				cat.init()
		}
	}

	/**
	 * Declares a `Boolean` config field, e.g. `val enableFeature by boolean(...)`.
	 *
	 * The field's id is the delegated property's name, snake_cased. On the client, the field is
	 * also registered with [ClientCategorySpec] so it renders as a toggle in the Cloth Config UI.
	 *
	 * @param title Display title shown in the Cloth Config UI.
	 * @param comment Optional comment written next to the field in the serialized file (JSON5/TOML)
	 * and used as the UI tooltip.
	 * @param default Value used until a stored/loaded value overrides it.
	 * @param resetKey Optional label for the UI's "reset to default" control.
	 * @return A read-only property delegate exposing the field's current value.
	 */
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

	/** Declares an `Int` config field. See [boolean] for parameter semantics. */
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

	/** Declares a `Long` config field. See [boolean] for parameter semantics. */
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

	/**
	 * Declares an `Int` config field rendered as a slider bounded by [min]/[max]. See [boolean]
	 * for the remaining parameter semantics.
	 *
	 * @param min Minimum value the slider allows.
	 * @param max Maximum value the slider allows.
	 * @param default Defaults to the midpoint of [min] and [max] if not given.
	 */
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

	/** Declares a `Long` config field rendered as a slider. See [intSlider] for parameter semantics. */
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

	/** Declares a `Float` config field. See [boolean] for parameter semantics. */
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

	/** Declares a `Double` config field. See [boolean] for parameter semantics. */
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

	/** Declares a `String` config field. See [boolean] for parameter semantics. */
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

	/**
	 * Declares a field that embeds another [CategorySpec] as a nested, serializable section. See
	 * [boolean] for the remaining parameter semantics.
	 *
	 * @param default Instance used until a stored/loaded value overrides it. Never mutated in
	 * place - deserialization always builds a fresh instance via [factory].
	 * @param factory Creates a new instance of the nested spec; used by the deserializer so
	 * loading a saved value never mutates [default].
	 */
	@Suppress("UNCHECKED_CAST")
	protected fun <T : CategorySpec> spec(
		title: Component,
		comment: Component? = null,
		default: T,
		factory: () -> T,
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
			// factory() must be used instead of returning `default` directly - the deserializer
			// stores its result back into `specs[id]`, so reusing `default` would let a saved
			// value mutate the shared default instance in place (breaking reset-to-default).
			types[id] = FieldType.Spec { factory() } as FieldType<T>
			(specs as MutableMap<String, T>).putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				(specs as MutableMap<String, T>).getOrPut(id) {
					default
				}
			}
		}

	/**
	 * Declares a field whose value is an entry of a vanilla [Registry], stored as the entry's
	 * [ResourceLocation] key. See [boolean] for the remaining parameter semantics.
	 *
	 * @param registry Registry the field's value is looked up in.
	 * @param subclass If given, narrows the entries offered in the UI to this runtime type; the
	 * stored key is still resolved against the full [registry].
	 */
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
				(registry.get(registries.getOrPut(id) {
					registry.getKey(default)!!
				}) ?: default) as R
			}
		}

	/**
	 * Declares a [CommonKeyCode] config field, rendered as a keybind picker. See [boolean] for
	 * the remaining parameter semantics.
	 */
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

	/**
	 * Declares a [Color] (ARGB) config field, rendered as a color picker. See [boolean] for the
	 * remaining parameter semantics.
	 *
	 * @param alpha Whether the picker allows editing the alpha channel.
	 */
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

	/**
	 * Declares a field whose value is one entry of the enum [kclass], rendered as a cycling
	 * selector over all of the enum's entries. See [boolean] for the remaining parameter
	 * semantics.
	 *
	 * @param kclass The enum type to select from.
	 */
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
			onClient {
				client.enumSelector(id, title, comment, kclass, default, resetKey)
			}
			types[id] = FieldType.EnumSelector(kclass)
			enums.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				enums.getOrPut(id) {
					default
				} as T
			}
		}

	/**
	 * Declares a field whose value is one of an arbitrary fixed set of [entries], rendered as a
	 * cycling selector. Unlike [enumSelector], the value type isn't required to be an `enum
	 * class`. See [boolean] for the remaining parameter semantics.
	 *
	 * @param kclass Runtime type of the selectable values.
	 * @param entries The fixed set of values the selector cycles through.
	 */
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
			onClient {
				client.selector(id, title, comment, kclass, default, entries, resetKey)
			}
			types[id] = FieldType.Selector(kclass)
			selectors.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				selectors.getOrPut(id) {
					default
				} as T
			}
		}

	/** Declares a `List<Int>` config field. See [boolean] for parameter semantics. */
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
			onClient {
				client.intList(id, title, comment, default, resetKey)
			}
			types[id] = FieldType.IntList
			intLists.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				intLists.getOrPut(id) {
					default
				}
			}
		}

	/** Declares a `List<Long>` config field. See [boolean] for parameter semantics. */
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
			onClient {
				client.longList(id, title, comment, default, resetKey)
			}
			types[id] = FieldType.LongList
			longLists.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				longLists.getOrPut(id) {
					default
				}
			}
		}

	/** Declares a `List<Float>` config field. See [boolean] for parameter semantics. */
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
			onClient {
				client.floatList(id, title, comment, default, resetKey)
			}
			types[id] = FieldType.FloatList
			floatLists.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				floatLists.getOrPut(id) {
					default
				}
			}
		}

	/** Declares a `List<Double>` config field. See [boolean] for parameter semantics. */
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
			onClient {
				client.doubleList(id, title, comment, default, resetKey)
			}
			types[id] = FieldType.DoubleList
			doubleLists.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				doubleLists.getOrPut(id) {
					default
				}
			}
		}

	/** Declares a `List<String>` config field. See [boolean] for parameter semantics. */
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
			onClient {
				client.stringList(id, title, comment, default, resetKey)
			}
			types[id] = FieldType.StringList
			stringLists.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				stringLists.getOrPut(id) {
					default
				}
			}
		}

	/** Declares a `List` of nested [CategorySpec] entries. See [spec] for parameter semantics. */
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
			onClient {
				client.specList(id, title, comment, default, factory, resetKey)
			}
			types[id] = FieldType.SpecList(factory) as FieldType<List<T>>
			(specLists).putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				(specLists as MutableMap<String, List<T>>).getOrPut(id) {
					default
				}
			}
		}

	/**
	 * Declares a `List` of [Registry] entries, stored as a list of [ResourceLocation] keys. See
	 * [registry] for parameter semantics.
	 *
	 * @param factory Used to produce a fallback value if a stored key no longer resolves in
	 * [registry] (e.g. the entry was removed by a datapack/mod update).
	 */
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
			onClient {
				client.registryList(id, title, comment, default, factory, registry, resetKey, subclass)
			}
			types[id] = FieldType.RegistryList
			registryLists.putIfAbsent(id, default.map { registry.getKey(it)!! })
			ReadOnlyProperty { _, _ ->
				@Suppress("UNCHECKED_CAST")
				registryLists.getOrPut(id) {
					default.map { registry.getKey(it)!! }
				}.map { registry.get(it) ?: factory() } as List<R>
			}
		}

	/** Declares a `List<CommonKeyCode>` config field. See [keycode] for parameter semantics. */
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
			onClient {
				client.keycodeList(id, title, comment, default, factory, resetKey)
			}
			types[id] = FieldType.KeyCodeList
			keycodeLists.putIfAbsent(id, default )
			ReadOnlyProperty { _, _ ->
				keycodeLists.getOrPut(id) {
					default
				}
			}
		}

	/** Declares a `List<Color>` config field. See [color] for parameter semantics. */
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
			onClient {
				client.colorList(id, title, comment, default, factory, resetKey)
			}
			types[id] = FieldType.ColorList
			colorLists.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				colorLists.getOrPut(id) {
					default
				}
			}
		}

	/** Declares a `Map<String, Int>` config field. See [boolean] for parameter semantics. */
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
			onClient {
				client.intMap(id, title, comment, default, resetKey)
			}
			types[id] = FieldType.IntMap
			intMaps.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				intMaps.getOrPut(id) {
					default
				}
			}
		}

	/** Declares a `Map<String, Long>` config field. See [boolean] for parameter semantics. */
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
			onClient {
				client.longMap(id, title, comment, default, resetKey)
			}
			types[id] = FieldType.LongMap
			longMaps.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				longMaps.getOrPut(id) {
					default
				}
			}
		}

	/** Declares a `Map<String, Float>` config field. See [boolean] for parameter semantics. */
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
			onClient {
				client.floatMap(id, title, comment, default, resetKey)
			}
			types[id] = FieldType.FloatMap
			floatMaps.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				floatMaps.getOrPut(id) {
					default
				}
			}
		}

	/** Declares a `Map<String, Double>` config field. See [boolean] for parameter semantics. */
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
			onClient {
				client.doubleMap(id, title, comment, default, resetKey)
			}
			types[id] = FieldType.DoubleMap
			doubleMaps.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				doubleMaps.getOrPut(id) {
					default
				}
			}
		}

	/** Declares a `Map<String, String>` config field. See [boolean] for parameter semantics. */
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
			onClient {
				client.stringMap(id, title, comment, default, resetKey)
			}
			types[id] = FieldType.StringMap
			stringMaps.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				stringMaps.getOrPut(id) {
					default
				}
			}
		}

	/** Declares a `Map` of nested [CategorySpec] entries. See [spec] for parameter semantics. */
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
			onClient {
				client.specMap(id, title, comment, default, factory, resetKey)
			}
			types[id] = FieldType.SpecMap(factory) as FieldType<Map<String, T>>
			(specMaps as MutableMap<String, Map<String, T>>).putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				(specMaps as MutableMap<String, Map<String, T>>).getOrPut(id) {
					default
				}
			}
		}

	/**
	 * Declares a `Map` of [Registry] entries, stored as a map of [ResourceLocation] keys. See
	 * [registryList] for parameter semantics.
	 */
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
			onClient {
				client.registryMap(id, title, comment, default, factory, registry, resetKey, subclass)
			}
			types[id] = FieldType.RegistryMap
			registryMaps.putIfAbsent(id, default.mapValues { registry.getKey(it.value)!! })
			ReadOnlyProperty { _, _ ->
				@Suppress("UNCHECKED_CAST")
				registryMaps.getOrPut(id) {
					default.mapValues { registry.getKey(it.value)!! }
				}.mapValues { registry.get(it.value) ?: factory() } as Map<String, R>
			}
		}

	/** Declares a `Map<String, CommonKeyCode>` config field. See [keycode] for parameter semantics. */
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
			onClient {
				client.keycodeMap(id, title, comment, default, factory, resetKey)
			}
			types[id] = FieldType.KeyCodeMap
			keycodeMaps.putIfAbsent(id, default)
			ReadOnlyProperty { _, _ ->
				keycodeMaps.getOrPut(id) {
					default
				}
			}
		}

	/** Declares a `Map<String, Color>` config field. See [color] for parameter semantics. */
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
		onClient {
			client.colorMap(id, title, comment, default, factory, resetKey)
		}
		types[id] = FieldType.ColorMap
		colorMaps.putIfAbsent(id, default)
		ReadOnlyProperty { _, _ ->
			colorMaps.getOrPut(id) {
				default
			}
		}
	}

	/**
	 * Serializes/deserializes a [CategorySpec] by walking its registered [types] and reading from
	 * or writing into the corresponding backing map (e.g. [booleans], [ints]).
	 */
	internal class ConfigCategorySerializer(val factory: () -> CategorySpec) :
		KSerializer<CategorySpec>
	{
		override val descriptor: SerialDescriptor by lazy {
			// init() must run before reading `types` - it's what registers this spec's own
			// subcategories (if any) into `types`; without it, a nested CategorySpec used as a
			// spec()/specList()/specMap() default that declares subcategories would silently
			// omit them from serialization.
			with(factory().also { it.init() })
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
				val spec = factory().also { it.init() }
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
			value.init()
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

