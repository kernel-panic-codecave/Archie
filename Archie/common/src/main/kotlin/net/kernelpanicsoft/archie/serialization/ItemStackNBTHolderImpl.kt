package net.kernelpanicsoft.archie.serialization

import net.kernelpanicsoft.archie.config.toSnakeCase
import net.kernelpanicsoft.archie.gui.item.SyncedItemHolder
import net.kernelpanicsoft.archie.transfer.ArchieEnergyStorage
import net.kernelpanicsoft.archie.transfer.ArchieFluidStorage
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
import kotlin.reflect.full.hasAnnotation

/**
 * [NBTHolder] implementation backing [NBTHolder.item], persisting field values into [stack]'s
 * [CustomData] component instead of an in-memory map.
 *
 * `@Sync`-annotated fields additionally push updates through [SyncedItemHolder] when the
 * delegating `thisRef` implements it - the [ItemStack]-holder equivalent of [NBTHolderImpl]'s
 * `thisRef is BlockEntity` handling.
 */
class ItemStackNBTHolderImpl(private val stack: ItemStack) : NBTHolder
{
	private val data: MutableMap<String, NbtTag> = mutableMapOf()
	private val itemStorage: MutableMap<String, ArchieItemStorage> = mutableMapOf()
	private val fluidStorage: MutableMap<String, ArchieFluidStorage> = mutableMapOf()
	private val energyStorage: MutableMap<String, ArchieEnergyStorage> = mutableMapOf()
	private val sync: MutableSet<String> = mutableSetOf()

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
			if (property.hasAnnotation<Sync>())
			{
				sync += property.name.toSnakeCase()
				if (thisRef is SyncedItemHolder)
					thisRef.registerSyncedProperty(property.name.toSnakeCase(), serializer)
			}

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
					if (thisRef is SyncedItemHolder && property.name.toSnakeCase() in sync)
						thisRef.onSyncedPropertyChanged(property.name.toSnakeCase(), serializer, value)
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
			if (property.hasAnnotation<Sync>())
			{
				sync += property.name.toSnakeCase()
				if (thisRef is SyncedItemHolder)
					thisRef.registerSyncedProperty(property.name.toSnakeCase(), ListSerializer(serializer))
			}

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
					if (thisRef is SyncedItemHolder && property.name.toSnakeCase() in sync)
						thisRef.onSyncedPropertyChanged(property.name.toSnakeCase(), ListSerializer(serializer), value.toList())
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
			if (property.hasAnnotation<Sync>())
			{
				sync += property.name.toSnakeCase()
				if (thisRef is SyncedItemHolder)
					thisRef.registerSyncedProperty(property.name.toSnakeCase(), MapSerializer(String.serializer(), serializer))
			}

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
					if (thisRef is SyncedItemHolder && property.name.toSnakeCase() in sync)
						thisRef.onSyncedPropertyChanged(property.name.toSnakeCase(), MapSerializer(String.serializer(), serializer), value.toMap())
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
			if (property.hasAnnotation<Sync>())
			{
				sync += property.name.toSnakeCase()
				if (thisRef is SyncedItemHolder)
					thisRef.registerSyncedProperty(property.name.toSnakeCase(), ArchieItemStorage.serializer())
			}
			val onUpdate = {
				if (thisRef is SyncedItemHolder && property.name.toSnakeCase() in sync)
					thisRef.onSyncedPropertyChanged(property.name.toSnakeCase(), ArchieItemStorage.serializer(), itemStorage[property.name.toSnakeCase()]!!)
				saveToStack()
			}
			val storage = ArchieItemStorage(size, onUpdate)
			// init { loadFromStack() } already ran (before this delegate even existed to be
			// hydrated by loadFromTag's itemStorage.forEach loop, unlike a BlockEntity's field
			// declarations - which all run in its constructor, before NBTBlockEntity.load() ever
			// calls loadFromTag) - so data may already hold this key's raw tag with nothing to
			// apply it to yet. Apply it now, directly, instead.
			data[property.name.toSnakeCase()]?.let { storage.readSnapshot(it) }
			itemStorage[property.name.toSnakeCase()] = storage
			ReadOnlyProperty { _, _ -> itemStorage[property.name.toSnakeCase()]!! }
		}
	}

	override fun fluidField(limit: Long, size: Int): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieFluidStorage>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			if (property.hasAnnotation<Sync>())
			{
				sync += property.name.toSnakeCase()
				if (thisRef is SyncedItemHolder)
					thisRef.registerSyncedProperty(property.name.toSnakeCase(), ArchieFluidStorage.serializer())
			}
			val onUpdate = {
				if (thisRef is SyncedItemHolder && property.name.toSnakeCase() in sync)
					thisRef.onSyncedPropertyChanged(property.name.toSnakeCase(), ArchieFluidStorage.serializer(), fluidStorage[property.name.toSnakeCase()]!!)
				saveToStack()
			}
			val storage = ArchieFluidStorage(limit, size, onUpdate)
			data[property.name.toSnakeCase()]?.let { storage.readSnapshot(it) }
			fluidStorage[property.name.toSnakeCase()] = storage
			ReadOnlyProperty { _, _ -> fluidStorage[property.name.toSnakeCase()]!! }
		}
	}

	override fun energyField(capacity: Long): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieEnergyStorage>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			if (property.hasAnnotation<Sync>())
			{
				sync += property.name.toSnakeCase()
				if (thisRef is SyncedItemHolder)
					thisRef.registerSyncedProperty(property.name.toSnakeCase(), ArchieEnergyStorage.serializer())
			}
			val onUpdate = {
				if (thisRef is SyncedItemHolder && property.name.toSnakeCase() in sync)
					thisRef.onSyncedPropertyChanged(property.name.toSnakeCase(), ArchieEnergyStorage.serializer(), energyStorage[property.name.toSnakeCase()]!!)
				saveToStack()
			}
			val storage = ArchieEnergyStorage(capacity, onUpdate)
			data[property.name.toSnakeCase()]?.let { storage.readSnapshot(it) }
			energyStorage[property.name.toSnakeCase()] = storage
			ReadOnlyProperty { _, _ -> energyStorage[property.name.toSnakeCase()]!! }
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
		return buildCompoundTag {
			data.filter { (key, _) -> key in sync }
				.forEach { (key, value) -> put(key, value) }
		}
	}

	override fun <T> updateProperty(propertyName: String, serializer: KSerializer<T>, value: T)
	{
		this.data[propertyName] = NBT.encodeToNbtTagRootless(serializer, value)
	}
}