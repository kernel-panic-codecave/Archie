package net.kernelpanicsoft.archie.serialization

import net.kernelpanicsoft.archie.config.toSnakeCase
import net.kernelpanicsoft.archie.transfer.ArchieItemStorage
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import net.benwoodworth.knbt.NbtTag
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * [NBTHolder] implementation backing [NBTHolder.item], persisting field values into [stack]'s
 * [CustomData] component instead of an in-memory map.
 */
class ItemStackNBTHolderImpl(private val stack: ItemStack) : NBTHolder
{
	private val data: MutableMap<String, NbtTag> = mutableMapOf()
	private val itemStorage: MutableMap<String, ArchieItemStorage> = mutableMapOf()

	init
	{
		loadFromStack()
	}

	override fun <T> field(
		serializer: KSerializer<T>,
		default: () -> T
	): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, T>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			val delegate = object : ReadWriteProperty<Any?, T>
			{
				override fun getValue(thisRef: Any?, property: KProperty<*>): T
				{
					loadFromStack()
					return runCatching {
						NBT.decodeFromNbtTagRootless(serializer, data.getOrPut(property.name.toSnakeCase()) {
							NBT.encodeToNbtTagRootless(serializer, default())
						})
					}.recover {
						val ret = default()
						data[property.name.toSnakeCase()] = NBT.encodeToNbtTagRootless(serializer, ret)
						ret
					}.getOrThrow()
				}

				override fun setValue(thisRef: Any?, property: KProperty<*>, value: T)
				{
					data[property.name.toSnakeCase()] = NBT.encodeToNbtTagRootless(serializer, value)
					saveToStack()
				}
			}
			if (property.name.toSnakeCase() !in data)
				delegate.setValue(thisRef, property, default())

			delegate
		}
	}

	override fun <T> listField(
		serializer: KSerializer<T>,
		default: () -> List<T>
	): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, MutableList<T>>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			val delegate = object : ReadWriteProperty<Any?, MutableList<T>>
			{
				override fun getValue(thisRef: Any?, property: KProperty<*>): MutableList<T>
				{
					loadFromStack()
					return ObservableList(runCatching {
						NBT.decodeFromNbtTagRootless(ListSerializer(serializer), data.getOrPut(property.name.toSnakeCase()) {
							NBT.encodeToNbtTagRootless(ListSerializer(serializer), default())
						})
					}.recover {
						val ret = default()
						data[property.name.toSnakeCase()] = NBT.encodeToNbtTagRootless(ListSerializer(serializer), ret)
						ret
					}.getOrThrow().toMutableList()) { list -> setValue(thisRef, property, list) }
				}

				override fun setValue(thisRef: Any?, property: KProperty<*>, value: MutableList<T>)
				{
					data[property.name.toSnakeCase()] = NBT.encodeToNbtTagRootless(ListSerializer(serializer), value)
					saveToStack()
				}
			}
			if (property.name.toSnakeCase() !in data)
				delegate.setValue(thisRef, property, default().toMutableList())

			delegate
		}
	}

	override fun <T> mapField(
		serializer: KSerializer<T>,
		default: () -> Map<String, T>
	): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, MutableMap<String, T>>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			val delegate = object : ReadWriteProperty<Any?, MutableMap<String, T>>
			{
				override fun getValue(thisRef: Any?, property: KProperty<*>): MutableMap<String, T>
				{
					loadFromStack()
					return ObservableMap(runCatching {
						NBT.decodeFromNbtTagRootless(MapSerializer(String.serializer(), serializer), data.getOrPut(property.name.toSnakeCase()) {
							NBT.encodeToNbtTagRootless(MapSerializer(String.serializer(), serializer), default())
						})
					}.recover {
						val ret = default()
						data[property.name.toSnakeCase()] = NBT.encodeToNbtTagRootless(MapSerializer(String.serializer(), serializer), ret)
						ret
					}.getOrThrow().toMutableMap()) { map -> setValue(thisRef, property, map) }
				}

				override fun setValue(thisRef: Any?, property: KProperty<*>, value: MutableMap<String, T>)
				{
					data[property.name.toSnakeCase()] = NBT.encodeToNbtTagRootless(MapSerializer(String.serializer(), serializer), value)
					saveToStack()
				}
			}
			if (property.name.toSnakeCase() !in data)
				delegate.setValue(thisRef, property, default().toMutableMap())

			delegate
		}
	}

	override fun itemField(size: Int): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieItemStorage>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			val onUpdate = {
				saveToStack()
			}
			itemStorage[property.name.toSnakeCase()] = ArchieItemStorage(size, onUpdate)
			ReadOnlyProperty { _, _ -> itemStorage[property.name.toSnakeCase()]!! }
		}
	}

	override fun loadFromTag(compoundTag: CompoundTag)
	{
		forEachTag(compoundTag) { (key, value) ->
			data[key] = value
		}
		itemStorage.forEach { (key, value) ->
			value.readSnapshot(data.getOrPut(key) {
				value.createSnapshot()
			})
		}
	}

	override fun saveToTag(compoundTag: CompoundTag)
	{
		mergeToCompoundTag(compoundTag) {
			itemStorage.forEach { (key, value) ->
				data[key] = value.createSnapshot()
			}
			data.forEach { (key, value) ->
				put(key, value)

			}
		}
	}

	fun loadFromStack()
	{
		stack.get(DataComponents.CUSTOM_DATA)?.apply {
			loadFromTag(copyTag())
		}
	}

	fun saveToStack()
	{
		stack.applyComponents(buildComponentPatch {
			set(DataComponents.CUSTOM_DATA, CustomData.of(CompoundTag().also { tag ->
				saveToTag(tag)
			}))
		})
	}

	override fun getSyncTag(): CompoundTag
	{
		return CompoundTag()
	}

	override fun <T> updateProperty(propertyName: String, serializer: KSerializer<T>, value: T)
	{
		this.data[propertyName] = NBT.encodeToNbtTagRootless(serializer, value)
	}
}