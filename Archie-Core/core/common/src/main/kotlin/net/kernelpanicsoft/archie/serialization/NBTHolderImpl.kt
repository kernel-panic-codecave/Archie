package net.kernelpanicsoft.archie.serialization

import net.kernelpanicsoft.archie.config.toSnakeCase
import net.kernelpanicsoft.archie.transfer.ArchieEnergyStorage
import net.kernelpanicsoft.archie.transfer.ArchieFluidStorage
import net.kernelpanicsoft.archie.transfer.ArchieItemStorage
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
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
					thisRef.getStateContainer().setPropertySerializer(property.name.toSnakeCase(), serializer)
				}
			}
			val delegate = object : ReadWriteProperty<Any?, MutableList<T>>
			{
				override fun getValue(thisRef: Any?, property: KProperty<*>): MutableList<T>
				{
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
					thisRef.getStateContainer().setPropertySerializer(property.name.toSnakeCase(), serializer)
				}
			}
			val delegate = object : ReadWriteProperty<Any?, MutableMap<String, T>>
			{
				override fun getValue(thisRef: Any?, property: KProperty<*>): MutableMap<String, T>
				{
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
		this.data[propertyName] = NBT.encodeToNbtTagRootless(serializer, value)
	}
}
