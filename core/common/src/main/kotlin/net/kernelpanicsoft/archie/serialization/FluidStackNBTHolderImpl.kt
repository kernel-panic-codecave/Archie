package net.kernelpanicsoft.archie.serialization

import net.kernelpanicsoft.archie.config.toSnakeCase
import net.kernelpanicsoft.archie.transfer.ArchieEnergyStorage
import net.kernelpanicsoft.archie.transfer.ArchieFluidStorage
import net.kernelpanicsoft.archie.transfer.ArchieItemStorage
import dev.architectury.fluid.FluidStack
import earth.terrarium.common_storage_lib.resources.fluid.FluidResource
import earth.terrarium.common_storage_lib.resources.item.ItemResource
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.NbtTag
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.block.entity.BlockEntity
import java.util.function.Predicate
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.hasAnnotation

/**
 * [NBTHolder] implementation backing [NBTHolder.fluid], persisting field values into [stack]'s
 * [CustomData] component instead of an in-memory map.
 */
class FluidStackNBTHolderImpl(private val stack: FluidStack) : NBTHolder
{
	private val data: MutableMap<String, NbtTag> = mutableMapOf()
	private val itemStorage: MutableMap<String, ArchieItemStorage> = mutableMapOf()
	private val fluidStorage: MutableMap<String, ArchieFluidStorage> = mutableMapOf()
	private val energyStorage: MutableMap<String, ArchieEnergyStorage> = mutableMapOf()
	private val nestedMapStorage: MutableMap<String, NestedNBTHolderMap<*>> = mutableMapOf()
	private val nestedMapFactories: MutableMap<String, (CompoundTag) -> NBTHolder?> = mutableMapOf()
	private val nestedListStorage: MutableMap<String, NestedNBTHolderList<*>> = mutableMapOf()
	private val nestedListFactories: MutableMap<String, (CompoundTag) -> NBTHolder?> = mutableMapOf()
	private val nestedHolderStorage: MutableMap<String, NestedNBTHolder<*>> = mutableMapOf()
	private val nestedHolderFactories: MutableMap<String, (CompoundTag) -> NBTHolder?> = mutableMapOf()
	private val itemMapStorage: MutableMap<String, ArchieStorageMap<ArchieItemStorage>> = mutableMapOf()
	private val itemListStorage: MutableMap<String, ArchieStorageList<ArchieItemStorage>> = mutableMapOf()
	private val fluidMapStorage: MutableMap<String, ArchieStorageMap<ArchieFluidStorage>> = mutableMapOf()
	private val fluidListStorage: MutableMap<String, ArchieStorageList<ArchieFluidStorage>> = mutableMapOf()
	private val energyMapStorage: MutableMap<String, ArchieStorageMap<ArchieEnergyStorage>> = mutableMapOf()
	private val energyListStorage: MutableMap<String, ArchieStorageList<ArchieEnergyStorage>> = mutableMapOf()

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
			val key = property.name.toSnakeCase()
			val delegate = object : ReadWriteProperty<Any?, T>
			{
				override fun getValue(thisRef: Any?, property: KProperty<*>): T
				{
					loadFromStack()
					return runCatching {
						SerializationManager.nbt.decodeFromNbtTagRootless(serializer, data.getOrPut(key) {
							SerializationManager.nbt.encodeToNbtTagRootless(serializer, default())
						})
					}.recover {
						val ret = default()
						data[key] = SerializationManager.nbt.encodeToNbtTagRootless(serializer, ret)
						ret
					}.getOrThrow()
				}

				override fun setValue(thisRef: Any?, property: KProperty<*>, value: T)
				{
					data[key] = SerializationManager.nbt.encodeToNbtTagRootless(serializer, value)
					saveToStack()
				}
			}
			if (key !in data)
				delegate.setValue(thisRef, property, default())

			delegate
		}
	}

	override fun <T> listField(
		serializer: KSerializer<T>,
		default: () -> List<T>
	): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ObservableList<T>>>
	{
		return PropertyDelegateProvider { _, property ->
			val key = property.name.toSnakeCase()
			// The single write path - see the equivalent in [NBTHolderImpl.listField].
			fun persist(value: List<T>)
			{
				data[key] = SerializationManager.nbt.encodeToNbtTagRootless(ListSerializer(serializer), value)
				saveToStack()
			}

			if (key !in data) persist(default())

			ReadOnlyProperty { _, _ ->
				loadFromStack()
				ObservableList(runCatching {
					SerializationManager.nbt.decodeFromNbtTagRootless(ListSerializer(serializer), data.getOrPut(key) {
						SerializationManager.nbt.encodeToNbtTagRootless(ListSerializer(serializer), default())
					})
				}.recover {
					val ret = default()
					data[key] = SerializationManager.nbt.encodeToNbtTagRootless(ListSerializer(serializer), ret)
					ret
				}.getOrThrow().toMutableList()) { list -> persist(list) }
			}
		}
	}

	override fun <T> mapField(
		serializer: KSerializer<T>,
		default: () -> Map<String, T>
	): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, MutableMap<String, T>>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			val key = property.name.toSnakeCase()
			val delegate = object : ReadWriteProperty<Any?, MutableMap<String, T>>
			{
				override fun getValue(thisRef: Any?, property: KProperty<*>): MutableMap<String, T>
				{
					loadFromStack()
					return ObservableMap(runCatching {
						SerializationManager.nbt.decodeFromNbtTagRootless(MapSerializer(String.serializer(), serializer), data.getOrPut(key) {
							SerializationManager.nbt.encodeToNbtTagRootless(MapSerializer(String.serializer(), serializer), default())
						})
					}.recover {
						val ret = default()
						data[key] = SerializationManager.nbt.encodeToNbtTagRootless(MapSerializer(String.serializer(), serializer), ret)
						ret
					}.getOrThrow().toMutableMap()) { map -> setValue(thisRef, property, map)}
				}

				override fun setValue(thisRef: Any?, property: KProperty<*>, value: MutableMap<String, T>)
				{
					data[key] = SerializationManager.nbt.encodeToNbtTagRootless(MapSerializer(String.serializer(), serializer), value)
					saveToStack()
				}
			}
			if (key !in data)
				delegate.setValue(thisRef, property, default().toMutableMap())

			delegate
		}
	}

	override fun <T : NBTHolder> nestedMapField(factory: (CompoundTag) -> T?): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, NestedNBTHolderMap<T>>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			val key = property.name.toSnakeCase()
			lateinit var map: NestedNBTHolderMap<T>
			map = NestedNBTHolderMap {
				data[key] = map.toNbtCompound()
				saveToStack()
			}
			(data[key] as? NbtCompound)?.let { map.loadFrom(it, factory) }
			nestedMapStorage[key] = map
			nestedMapFactories[key] = factory
			ReadOnlyProperty { _, _ -> map }
		}
	}

	override fun <T : NBTHolder> nestedListField(factory: (CompoundTag) -> T?): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, NestedNBTHolderList<T>>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			val key = property.name.toSnakeCase()
			lateinit var list: NestedNBTHolderList<T>
			list = NestedNBTHolderList {
				data[key] = list.toNbtCompound()
				saveToStack()
			}
			(data[key] as? NbtCompound)?.let { list.loadFrom(it, factory) }
			nestedListStorage[key] = list
			nestedListFactories[key] = factory
			ReadOnlyProperty { _, _ -> list }
		}
	}

	override fun <T : NBTHolder> nestedField(factory: (CompoundTag) -> T?): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, NestedNBTHolder<T>>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			val key = property.name.toSnakeCase()
			lateinit var holder: NestedNBTHolder<T>
			holder = NestedNBTHolder {
				// Removed rather than stored empty when the holder is unset - see toNbtCompoundOrNull.
				holder.toNbtCompoundOrNull().let { if (it == null) data.remove(key) else data[key] = it }
				saveToStack()
			}
			(data[key] as? NbtCompound)?.let { holder.loadFrom(it, factory) }
			nestedHolderStorage[key] = holder
			nestedHolderFactories[key] = factory
			ReadOnlyProperty { _, _ -> holder }
		}
	}

	override fun itemField(
		size: Int,
		filter: Predicate<ItemResource>,
		onUpdate: (() -> Unit)?
	): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieItemStorage>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			val key = property.name.toSnakeCase()
			val internalOnUpdate: () -> Unit = {
				saveToStack()
				onUpdate?.invoke()
			}
			itemStorage[key] = ArchieItemStorage(size, filter, internalOnUpdate)
			ReadOnlyProperty { _, _ -> itemStorage[key]!! }
		}
	}

	override fun itemMapField(
		size: Int,
		filter: Predicate<ItemResource>,
		onUpdate: (() -> Unit)?
	): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieStorageMap<ArchieItemStorage>>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			val key = property.name.toSnakeCase()
			lateinit var map: ArchieStorageMap<ArchieItemStorage>
			val internalOnChange: () -> Unit = {
				data[key] = map.toNbtCompound()
				saveToStack()
				onUpdate?.invoke()
			}
			map = ArchieStorageMap({ ArchieItemStorage(size, filter, internalOnChange) }, internalOnChange)
			(data[key] as? NbtCompound)?.let { map.loadFrom(it) }
			itemMapStorage[key] = map
			ReadOnlyProperty { _, _ -> map }
		}
	}

	override fun itemListField(
		size: Int,
		filter: Predicate<ItemResource>,
		onUpdate: (() -> Unit)?
	): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieStorageList<ArchieItemStorage>>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			val key = property.name.toSnakeCase()
			lateinit var list: ArchieStorageList<ArchieItemStorage>
			val internalOnChange: () -> Unit = {
				data[key] = list.toNbtCompound()
				saveToStack()
				onUpdate?.invoke()
			}
			list = ArchieStorageList({ ArchieItemStorage(size, filter, internalOnChange) }, internalOnChange)
			(data[key] as? NbtCompound)?.let { list.loadFrom(it) }
			itemListStorage[key] = list
			ReadOnlyProperty { _, _ -> list }
		}
	}

	override fun fluidField(
		limit: Long,
		size: Int,
		filter: Predicate<FluidResource>,
		onUpdate: (() -> Unit)?
	): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieFluidStorage>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			val key = property.name.toSnakeCase()
			val internalOnUpdate: () -> Unit = {
				saveToStack()
				onUpdate?.invoke()
			}
			fluidStorage[key] = ArchieFluidStorage(limit, size, filter, internalOnUpdate)
			ReadOnlyProperty { _, _ -> fluidStorage[key]!! }
		}
	}

	override fun fluidMapField(
		limit: Long,
		size: Int,
		filter: Predicate<FluidResource>,
		onUpdate: (() -> Unit)?
	): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieStorageMap<ArchieFluidStorage>>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			val key = property.name.toSnakeCase()
			lateinit var map: ArchieStorageMap<ArchieFluidStorage>
			val internalOnChange: () -> Unit = {
				data[key] = map.toNbtCompound()
				saveToStack()
				onUpdate?.invoke()
			}
			map = ArchieStorageMap({ ArchieFluidStorage(limit, size, filter, internalOnChange) }, internalOnChange)
			(data[key] as? NbtCompound)?.let { map.loadFrom(it) }
			fluidMapStorage[key] = map
			ReadOnlyProperty { _, _ -> map }
		}
	}

	override fun fluidListField(
		limit: Long,
		size: Int,
		filter: Predicate<FluidResource>,
		onUpdate: (() -> Unit)?
	): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieStorageList<ArchieFluidStorage>>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			val key = property.name.toSnakeCase()
			lateinit var list: ArchieStorageList<ArchieFluidStorage>
			val internalOnChange: () -> Unit = {
				data[key] = list.toNbtCompound()
				saveToStack()
				onUpdate?.invoke()
			}
			list = ArchieStorageList({ ArchieFluidStorage(limit, size, filter, internalOnChange) }, internalOnChange)
			(data[key] as? NbtCompound)?.let { list.loadFrom(it) }
			fluidListStorage[key] = list
			ReadOnlyProperty { _, _ -> list }
		}
	}

	override fun energyField(capacity: Long, onUpdate: (() -> Unit)?): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieEnergyStorage>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			val key = property.name.toSnakeCase()
			val internalOnUpdate: () -> Unit = {
				saveToStack()
				onUpdate?.invoke()
			}
			energyStorage[key] = ArchieEnergyStorage(capacity, internalOnUpdate)
			ReadOnlyProperty { _, _ -> energyStorage[key]!! }
		}
	}

	override fun energyMapField(capacity: Long, onUpdate: (() -> Unit)?): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieStorageMap<ArchieEnergyStorage>>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			val key = property.name.toSnakeCase()
			lateinit var map: ArchieStorageMap<ArchieEnergyStorage>
			val internalOnChange: () -> Unit = {
				data[key] = map.toNbtCompound()
				saveToStack()
				onUpdate?.invoke()
			}
			map = ArchieStorageMap({ ArchieEnergyStorage(capacity, internalOnChange) }, internalOnChange)
			(data[key] as? NbtCompound)?.let { map.loadFrom(it) }
			energyMapStorage[key] = map
			ReadOnlyProperty { _, _ -> map }
		}
	}

	override fun energyListField(capacity: Long, onUpdate: (() -> Unit)?): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieStorageList<ArchieEnergyStorage>>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			val key = property.name.toSnakeCase()
			lateinit var list: ArchieStorageList<ArchieEnergyStorage>
			val internalOnChange: () -> Unit = {
				data[key] = list.toNbtCompound()
				saveToStack()
				onUpdate?.invoke()
			}
			list = ArchieStorageList({ ArchieEnergyStorage(capacity, internalOnChange) }, internalOnChange)
			(data[key] as? NbtCompound)?.let { list.loadFrom(it) }
			energyListStorage[key] = list
			ReadOnlyProperty { _, _ -> list }
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
		fluidStorage.forEach { (key, value) ->
			value.readSnapshot(data.getOrPut(key) {
				value.createSnapshot()
			})
		}
		energyStorage.forEach { (key, value) ->
			value.readSnapshot(data.getOrPut(key) {
				value.createSnapshot()
			})
		}
		nestedMapStorage.forEach { (key, map) ->
			map.loadFrom(data[key] as? NbtCompound, nestedMapFactories.getValue(key))
		}
		nestedListStorage.forEach { (key, list) ->
			list.loadFrom(data[key] as? NbtCompound, nestedListFactories.getValue(key))
		}
		nestedHolderStorage.forEach { (key, holder) ->
			holder.loadFrom(data[key] as? NbtCompound, nestedHolderFactories.getValue(key))
		}
		itemMapStorage.forEach { (key, map) -> map.loadFrom(data[key] as? NbtCompound) }
		itemListStorage.forEach { (key, list) -> list.loadFrom(data[key] as? NbtCompound) }
		fluidMapStorage.forEach { (key, map) -> map.loadFrom(data[key] as? NbtCompound) }
		fluidListStorage.forEach { (key, list) -> list.loadFrom(data[key] as? NbtCompound) }
		energyMapStorage.forEach { (key, map) -> map.loadFrom(data[key] as? NbtCompound) }
		energyListStorage.forEach { (key, list) -> list.loadFrom(data[key] as? NbtCompound) }
	}

	override fun saveToTag(compoundTag: CompoundTag)
	{
		mergeToCompoundTag(compoundTag) {
			itemStorage.forEach { (key, value) ->
				data[key] = value.createSnapshot()
			}
			fluidStorage.forEach { (key, value) ->
				data[key] = value.createSnapshot()
			}
			energyStorage.forEach { (key, value) ->
				data[key] = value.createSnapshot()
			}
			nestedMapStorage.forEach { (key, map) ->
				data[key] = map.toNbtCompound()
			}
			nestedListStorage.forEach { (key, list) ->
				data[key] = list.toNbtCompound()
			}
			nestedHolderStorage.forEach { (key, holder) ->
				holder.toNbtCompoundOrNull().let { if (it == null) data.remove(key) else data[key] = it }
			}
			itemMapStorage.forEach { (key, map) -> data[key] = map.toNbtCompound() }
			itemListStorage.forEach { (key, list) -> data[key] = list.toNbtCompound() }
			fluidMapStorage.forEach { (key, map) -> data[key] = map.toNbtCompound() }
			fluidListStorage.forEach { (key, list) -> data[key] = list.toNbtCompound() }
			energyMapStorage.forEach { (key, map) -> data[key] = map.toNbtCompound() }
			energyListStorage.forEach { (key, list) -> data[key] = list.toNbtCompound() }
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
		this.data[propertyName] = SerializationManager.nbt.encodeToNbtTagRootless(serializer, value)
	}
}