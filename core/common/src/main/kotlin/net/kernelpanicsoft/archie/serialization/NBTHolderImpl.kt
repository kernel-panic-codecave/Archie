package net.kernelpanicsoft.archie.serialization

import net.kernelpanicsoft.archie.config.toSnakeCase
import net.kernelpanicsoft.archie.transfer.ArchieEnergyStorage
import net.kernelpanicsoft.archie.transfer.ArchieFluidStorage
import net.kernelpanicsoft.archie.transfer.ArchieItemStorage
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.NbtTag
import net.kernelpanicsoft.archie.gui.blockentity.getStateContainer
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntity
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.hasAnnotation

/**
 * Default [NBTHolder] implementation backing [NBTHolder.create]. Field values are cached
 * in-memory as knbt tags keyed by the delegated property's snake_case name; properties
 * annotated [Sync] additionally push updates through [net.kernelpanicsoft.archie.gui.blockentity.BlockEntityStateManager]-backed state
 * containers when the holder is attached to a [BlockEntity].
 */
class NBTHolderImpl : NBTHolder
{
	private val data: MutableMap<String, NbtTag> = mutableMapOf()
	private val itemStorage: MutableMap<String, ArchieItemStorage> = mutableMapOf()
	private val fluidStorage: MutableMap<String, ArchieFluidStorage> = mutableMapOf()
	private val energyStorage: MutableMap<String, ArchieEnergyStorage> = mutableMapOf()
	private val nestedMapStorage: MutableMap<String, NestedNBTHolderMap> = mutableMapOf()
	private val nestedMapFactories: MutableMap<String, (CompoundTag) -> NBTHolder> = mutableMapOf()
	private val nestedListStorage: MutableMap<String, NestedNBTHolderList> = mutableMapOf()
	private val nestedListFactories: MutableMap<String, (CompoundTag) -> NBTHolder> = mutableMapOf()
	private val nestedHolderStorage: MutableMap<String, NBTHolder> = mutableMapOf()
	private val itemMapStorage: MutableMap<String, ArchieStorageMap<ArchieItemStorage>> = mutableMapOf()
	private val itemListStorage: MutableMap<String, ArchieStorageList<ArchieItemStorage>> = mutableMapOf()
	private val fluidMapStorage: MutableMap<String, ArchieStorageMap<ArchieFluidStorage>> = mutableMapOf()
	private val fluidListStorage: MutableMap<String, ArchieStorageList<ArchieFluidStorage>> = mutableMapOf()
	private val energyMapStorage: MutableMap<String, ArchieStorageMap<ArchieEnergyStorage>> = mutableMapOf()
	private val energyListStorage: MutableMap<String, ArchieStorageList<ArchieEnergyStorage>> = mutableMapOf()
	private val sync: MutableSet<String> = mutableSetOf()

	override fun <T> field(
		serializer: KSerializer<T>,
		default: () -> T
	): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, T>>
	{

		return PropertyDelegateProvider { thisRef, property ->
			if (property.hasAnnotation<Sync>())
			{
				sync += property.name.toSnakeCase()
				if (thisRef is BlockEntity)
				{
					thisRef.getStateContainer().setPropertySerializer(property.name.toSnakeCase(), serializer)
				}
			}

			val delegate = object : ReadWriteProperty<Any?, T>
			{
				override fun getValue(thisRef: Any?, property: KProperty<*>): T
				{
					return runCatching {
						SerializationManager.nbt.decodeFromNbtTagRootless(serializer, data.getOrPut(property.name.toSnakeCase()) {
							SerializationManager.nbt.encodeToNbtTagRootless(serializer, default())
						})
					}.recover {
						val ret = default()
						data[property.name.toSnakeCase()] = SerializationManager.nbt.encodeToNbtTagRootless(serializer, ret)
						ret
					}.getOrThrow()
				}

				override fun setValue(thisRef: Any?, property: KProperty<*>, value: T)
				{
					data[property.name.toSnakeCase()] = SerializationManager.nbt.encodeToNbtTagRootless(serializer, value)
					if (thisRef is BlockEntity)
					{
						if (property.name.toSnakeCase() in sync)
							thisRef.getStateContainer().updateProperty(property.name.toSnakeCase(), value)
						thisRef.setChanged()
					}
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
				if (thisRef is BlockEntity)
				{
					thisRef.getStateContainer().setPropertySerializer(property.name.toSnakeCase(), ListSerializer(serializer))
				}
			}
			val delegate = object : ReadWriteProperty<Any?, MutableList<T>>
			{
				override fun getValue(thisRef: Any?, property: KProperty<*>): MutableList<T>
				{
					return ObservableList(runCatching {
						SerializationManager.nbt.decodeFromNbtTagRootless(ListSerializer(serializer), data.getOrPut(property.name.toSnakeCase()) {
							SerializationManager.nbt.encodeToNbtTagRootless(ListSerializer(serializer), default())
						})
					}.recover {
						val ret = default()
						data[property.name.toSnakeCase()] = SerializationManager.nbt.encodeToNbtTagRootless(ListSerializer(serializer), ret)
						ret
					}.getOrThrow().toMutableList()) { list -> setValue(thisRef, property, list) }
				}

				override fun setValue(thisRef: Any?, property: KProperty<*>, value: MutableList<T>)
				{
					data[property.name.toSnakeCase()] = SerializationManager.nbt.encodeToNbtTagRootless(ListSerializer(serializer), value)
					if (thisRef is BlockEntity)
					{
						if (property.name.toSnakeCase() in sync)
							thisRef.getStateContainer().updateProperty(property.name.toSnakeCase(), value)
						thisRef.setChanged()
					}
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
				if (thisRef is BlockEntity)
				{
					thisRef.getStateContainer().setPropertySerializer(property.name.toSnakeCase(), MapSerializer(String.serializer(), serializer))
				}
			}
			val delegate = object : ReadWriteProperty<Any?, MutableMap<String, T>>
			{
				override fun getValue(thisRef: Any?, property: KProperty<*>): MutableMap<String, T>
				{
					return ObservableMap(runCatching {
						SerializationManager.nbt.decodeFromNbtTagRootless(MapSerializer(String.serializer(), serializer), data.getOrPut(property.name.toSnakeCase()) {
							SerializationManager.nbt.encodeToNbtTagRootless(MapSerializer(String.serializer(), serializer), default())
						})
					}.recover {
						val ret = default()
						data[property.name.toSnakeCase()] = SerializationManager.nbt.encodeToNbtTagRootless(MapSerializer(String.serializer(), serializer), ret)
						ret
					}.getOrThrow().toMutableMap()) { map -> setValue(thisRef, property, map) }
				}

				override fun setValue(thisRef: Any?, property: KProperty<*>, value: MutableMap<String, T>)
				{
					data[property.name.toSnakeCase()] = SerializationManager.nbt.encodeToNbtTagRootless(MapSerializer(String.serializer(), serializer), value)
					if (thisRef is BlockEntity)
					{
						if (property.name.toSnakeCase() in sync)
							thisRef.getStateContainer().updateProperty(property.name.toSnakeCase(), value)
						thisRef.setChanged()
					}
				}
			}
			if (property.name.toSnakeCase() !in data)
				delegate.setValue(thisRef, property, default().toMutableMap())
			delegate
		}
	}

	override fun nestedMapField(factory: (CompoundTag) -> NBTHolder): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, NestedNBTHolderMap>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			val key = property.name.toSnakeCase()
			if (property.hasAnnotation<Sync>())
			{
				sync += key
				// Individual nested fields aren't wired into BlockEntityStateManager for live GUI
				// observation - only the map as a whole round-trips through saveToTag/loadFromTag/
				// getSyncTag, same as itemField/fluidField/energyField below.
			}
			lateinit var map: NestedNBTHolderMap
			map = NestedNBTHolderMap {
				data[key] = map.toNbtCompound()
				if (thisRef is BlockEntity) thisRef.setChanged()
			}
			(data[key] as? NbtCompound)?.let { map.loadFrom(it, factory) }
			nestedMapStorage[key] = map
			nestedMapFactories[key] = factory
			ReadOnlyProperty { _, _ -> map }
		}
	}

	override fun nestedListField(factory: (CompoundTag) -> NBTHolder): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, NestedNBTHolderList>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			val key = property.name.toSnakeCase()
			if (property.hasAnnotation<Sync>()) sync += key
			lateinit var list: NestedNBTHolderList
			list = NestedNBTHolderList {
				data[key] = list.toNbtCompound()
				if (thisRef is BlockEntity) thisRef.setChanged()
			}
			(data[key] as? NbtCompound)?.let { list.loadFrom(it, factory) }
			nestedListStorage[key] = list
			nestedListFactories[key] = factory
			ReadOnlyProperty { _, _ -> list }
		}
	}

	override fun <T : NBTHolder> nestedField(factory: () -> T): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, T>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			val key = property.name.toSnakeCase()
			if (property.hasAnnotation<Sync>()) sync += key
			val holder = factory()
			(data[key] as? NbtCompound)?.toMinecraft?.let { holder.loadFromTag(it) }
			nestedHolderStorage[key] = holder
			ReadOnlyProperty { _, _ -> holder }
		}
	}

	override fun itemField(size: Int): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieItemStorage>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			if (property.hasAnnotation<Sync>())
			{
				sync += property.name.toSnakeCase()
				if (thisRef is BlockEntity)
				{
					thisRef.getStateContainer().setPropertySerializer(property.name.toSnakeCase(), ArchieItemStorage.serializer())
				}
			}
			val onUpdate = when (thisRef)
			{
				is BlockEntity -> ({
					if (property.name.toSnakeCase() in sync)
						thisRef.getStateContainer().updateProperty(property.name.toSnakeCase(), itemStorage[property.name.toSnakeCase()])
					thisRef.setChanged()
				})
				else -> ({})
			}
			itemStorage[property.name.toSnakeCase()] = ArchieItemStorage(size, onUpdate)
			ReadOnlyProperty { _, _ -> itemStorage[property.name.toSnakeCase()]!! }
		}
	}

	override fun itemMapField(size: Int): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieStorageMap<ArchieItemStorage>>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			val key = property.name.toSnakeCase()
			lateinit var map: ArchieStorageMap<ArchieItemStorage>
			map = ArchieStorageMap({ ArchieItemStorage(size) }) {
				data[key] = map.toNbtCompound()
				if (thisRef is BlockEntity) thisRef.setChanged()
			}
			(data[key] as? NbtCompound)?.let { map.loadFrom(it) }
			itemMapStorage[key] = map
			ReadOnlyProperty { _, _ -> map }
		}
	}

	override fun itemListField(size: Int): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieStorageList<ArchieItemStorage>>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			val key = property.name.toSnakeCase()
			lateinit var list: ArchieStorageList<ArchieItemStorage>
			list = ArchieStorageList({ ArchieItemStorage(size) }) {
				data[key] = list.toNbtCompound()
				if (thisRef is BlockEntity) thisRef.setChanged()
			}
			(data[key] as? NbtCompound)?.let { list.loadFrom(it) }
			itemListStorage[key] = list
			ReadOnlyProperty { _, _ -> list }
		}
	}

	override fun fluidField(limit: Long, size: Int): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieFluidStorage>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			if (property.hasAnnotation<Sync>())
			{
				sync += property.name.toSnakeCase()
				if (thisRef is BlockEntity)
				{
					thisRef.getStateContainer().setPropertySerializer(property.name.toSnakeCase(), ArchieFluidStorage.serializer())
				}
			}
			val onUpdate = when (thisRef)
			{
				is BlockEntity -> ({
					if (property.name.toSnakeCase() in sync)
						thisRef.getStateContainer().updateProperty(property.name.toSnakeCase(), fluidStorage[property.name.toSnakeCase()])
					thisRef.setChanged()
				})
				else -> ({})
			}
			fluidStorage[property.name.toSnakeCase()] = ArchieFluidStorage(limit, size, onUpdate)
			ReadOnlyProperty { _, _ -> fluidStorage[property.name.toSnakeCase()]!! }
		}
	}

	override fun fluidMapField(limit: Long, size: Int): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieStorageMap<ArchieFluidStorage>>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			val key = property.name.toSnakeCase()
			lateinit var map: ArchieStorageMap<ArchieFluidStorage>
			map = ArchieStorageMap({ ArchieFluidStorage(limit, size) }) {
				data[key] = map.toNbtCompound()
				if (thisRef is BlockEntity) thisRef.setChanged()
			}
			(data[key] as? NbtCompound)?.let { map.loadFrom(it) }
			fluidMapStorage[key] = map
			ReadOnlyProperty { _, _ -> map }
		}
	}

	override fun fluidListField(limit: Long, size: Int): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieStorageList<ArchieFluidStorage>>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			val key = property.name.toSnakeCase()
			lateinit var list: ArchieStorageList<ArchieFluidStorage>
			list = ArchieStorageList({ ArchieFluidStorage(limit, size) }) {
				data[key] = list.toNbtCompound()
				if (thisRef is BlockEntity) thisRef.setChanged()
			}
			(data[key] as? NbtCompound)?.let { list.loadFrom(it) }
			fluidListStorage[key] = list
			ReadOnlyProperty { _, _ -> list }
		}
	}

	override fun energyField(capacity: Long): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieEnergyStorage>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			if (property.hasAnnotation<Sync>())
			{
				sync += property.name.toSnakeCase()
				if (thisRef is BlockEntity)
				{
					thisRef.getStateContainer().setPropertySerializer(property.name.toSnakeCase(), ArchieEnergyStorage.serializer())
				}
			}
			val onUpdate = when (thisRef)
			{
				is BlockEntity -> ({
					if (property.name.toSnakeCase() in sync)
						thisRef.getStateContainer().updateProperty(property.name.toSnakeCase(), energyStorage[property.name.toSnakeCase()])
					thisRef.setChanged()
				})
				else -> ({})
			}
			energyStorage[property.name.toSnakeCase()] = ArchieEnergyStorage(capacity, onUpdate)
			ReadOnlyProperty { _, _ -> energyStorage[property.name.toSnakeCase()]!! }
		}
	}

	override fun energyMapField(capacity: Long): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieStorageMap<ArchieEnergyStorage>>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			val key = property.name.toSnakeCase()
			lateinit var map: ArchieStorageMap<ArchieEnergyStorage>
			map = ArchieStorageMap({ ArchieEnergyStorage(capacity) }) {
				data[key] = map.toNbtCompound()
				if (thisRef is BlockEntity) thisRef.setChanged()
			}
			(data[key] as? NbtCompound)?.let { map.loadFrom(it) }
			energyMapStorage[key] = map
			ReadOnlyProperty { _, _ -> map }
		}
	}

	override fun energyListField(capacity: Long): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieStorageList<ArchieEnergyStorage>>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			val key = property.name.toSnakeCase()
			lateinit var list: ArchieStorageList<ArchieEnergyStorage>
			list = ArchieStorageList({ ArchieEnergyStorage(capacity) }) {
				data[key] = list.toNbtCompound()
				if (thisRef is BlockEntity) thisRef.setChanged()
			}
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
			(data[key] as? NbtCompound)?.toMinecraft?.let { holder.loadFromTag(it) }
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
				data[key] = CompoundTag().also { holder.saveToTag(it) }.fromMinecraft
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

	override fun getSyncTag(): CompoundTag
	{
		return buildCompoundTag {
			data.filter { (key, _) -> key in sync }
				.forEach { (key, value) ->
					put(key, value)
				}
		}
	}

	override fun <T> updateProperty(propertyName: String, serializer: KSerializer<T>, value: T)
	{
		this.data[propertyName] = SerializationManager.nbt.encodeToNbtTagRootless(serializer, value)
	}
}
