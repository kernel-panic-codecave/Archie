package com.withertech.archie.serialization

import com.withertech.archie.config.toSnakeCase
import com.withertech.archie.transfer.ArchieItemStorage
import kotlinx.serialization.KSerializer
import net.benwoodworth.knbt.NbtTag
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntity
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.hasAnnotation

class NBTHolderImpl : NBTHolder
{
	private val data: MutableMap<String, NbtTag> = mutableMapOf()
	private val itemStorage: MutableMap<String, ArchieItemStorage> = mutableMapOf()
	private val sync: MutableSet<String> = mutableSetOf()

	override fun <T> field(
		serializer: KSerializer<T>,
		default: () -> T
	): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, T>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			if (property.hasAnnotation<Sync>())
				sync += property.name.toSnakeCase()
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
				}
			}
			if (property.name.toSnakeCase() !in data)
				delegate.setValue(thisRef, property, default())
			delegate
		}
	}

	override fun itemField(size: Int): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieItemStorage>>
	{
		return PropertyDelegateProvider { thisRef, property ->
			if (property.hasAnnotation<Sync>())
				sync += property.name.toSnakeCase()
			val onUpdate = when (thisRef)
			{
				is BlockEntity -> ({ thisRef.setChanged() })
				else -> ({})
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

	override fun getSyncTag(): CompoundTag
	{
		return buildCompoundTag {
			data.filter { (key, _) -> key in sync }
				.forEach { (key, value) ->
					put(key, value)
				}
		}
	}
}