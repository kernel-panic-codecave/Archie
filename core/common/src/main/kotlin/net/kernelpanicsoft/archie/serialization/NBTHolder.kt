package net.kernelpanicsoft.archie.serialization

import net.kernelpanicsoft.archie.transfer.ArchieEnergyStorage
import net.kernelpanicsoft.archie.transfer.ArchieFluidStorage
import net.kernelpanicsoft.archie.transfer.ArchieItemStorage
import dev.architectury.fluid.FluidStack
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.serializer
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import kotlin.collections.listOf
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * An interface for managing NBT-backed fields on block entities, item stacks, or fluid stacks.
 *
 * [NBTHolder] provides a property-delegation API that serializes field values to/from a
 * [CompoundTag] using kotlinx.serialization. Each delegated field is keyed by its Kotlin
 * property name.
 *
 * ### Usage on a block entity
 * ```kotlin
 * class MyBlockEntity(pos, state) : NBTBlockEntity(pos, state) {
 *     var count by nbt.intField()
 *     var label by nbt.stringField { "default" }
 *     val items by nbt.itemField(9)        // 9-slot inventory
 *     val tank by nbt.fluidField(FluidStack.bucketAmount() * 4)  // 1 tank slot, 4 buckets
 *     val energy by nbt.energyField(10_000) // a single energy buffer
 * }
 * ```
 *
 * Obtain instances via [NBTHolder.create], [NBTHolder.item], or [NBTHolder.fluid].
 */
@Suppress("unused")
interface NBTHolder
{
	/**
	 * Declares a read-write field backed by [serializer], keyed by the delegated property's name.
	 * [default] supplies the value used before the field has been loaded/set.
	 */
	fun <T> field(serializer: KSerializer<T>, default: () -> T): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, T>>

	/** Declares a mutable-list field backed by [serializer], keyed by the delegated property's name. */
	fun <T> listField(serializer: KSerializer<T>, default: () -> List<T>): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, MutableList<T>>>
	/** Declares a mutable-map (keyed by [String]) field backed by [serializer], keyed by the delegated property's name. */
	fun <T> mapField(serializer: KSerializer<T>, default: () -> Map<String, T>): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, MutableMap<String, T>>>

	/**
	 * Declares a [NestedNBTHolderMap] field, keyed by the delegated property's name. Unlike
	 * [mapField], entries aren't one fixed kotlinx.serialization-compatible value type - each is
	 * its own self-contained [NBTHolder], built fresh via [factory] either when first inserted
	 * ([NestedNBTHolderMap.getOrPut]) or, when loading a previously-saved entry back, from that
	 * entry's own raw sub-[CompoundTag] (so [factory] can inspect it - e.g. a stored "kind" field -
	 * to decide which concrete nested holder type to reconstruct). [T] is [factory]'s return type -
	 * the common upper bound every entry shares, letting a caller declare e.g.
	 * `NestedNBTHolderMap<HookHolderState>` instead of always getting back the base [NBTHolder]
	 * type and having to cast every entry manually.
	 */
	fun <T : NBTHolder> nestedMapField(factory: (CompoundTag) -> T?): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, NestedNBTHolderMap<T>>>

	/**
	 * Declares a [NestedNBTHolderList] field, keyed by the delegated property's name - the
	 * index-ordered counterpart to [nestedMapField]; see its KDoc for [factory]'s role and [T]'s.
	 */
	fun <T : NBTHolder> nestedListField(factory: (CompoundTag) -> T?): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, NestedNBTHolderList<T>>>

	/**
	 * Declares a single nested [NBTHolder] field, keyed by the delegated property's name and built
	 * once via [factory] - the scalar counterpart to [nestedMapField]/[nestedListField]. Unlike
	 * those, there's exactly one fixed-type entry, so no per-entry type discrimination is needed:
	 * [factory] takes no tag to inspect.
	 */
	fun <T : NBTHolder> nestedField(factory: (CompoundTag) -> T?): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, NestedNBTHolder<T>>>

	/**
	 * Declares an [ArchieItemStorage] field with [size] slots, keyed by the delegated property's
	 * name. [onUpdate], if given, runs in addition to (not instead of) this holder's own internal
	 * bookkeeping (persistence, `@Sync` propagation, `BlockEntity.setChanged`, ...) every time
	 * [ArchieItemStorage.update] fires for this specific field - a caller-supplied "something in
	 * this inventory changed" hook, without needing to poll it or re-derive the same notification
	 * some other way.
	 */
	fun itemField(size: Int, onUpdate: (() -> Unit)? = null): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieItemStorage>>

	/**
	 * Declares an [ArchieStorageMap] of [ArchieItemStorage]s, each with [size] slots, keyed by the
	 * delegated property's name. [onUpdate], if given, runs whenever the map's own structure
	 * changes (an entry added/removed/cleared) *or* any individual entry's own contents change -
	 * every entry the map creates is wired with the map's own persistence trigger as well, so a
	 * mutation deep inside one entry is never silently unpersisted.
	 */
	fun itemMapField(size: Int, onUpdate: (() -> Unit)? = null): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieStorageMap<ArchieItemStorage>>>

	/** Declares an [ArchieStorageList] of [ArchieItemStorage]s, each with [size] slots, keyed by the delegated property's name. See [itemMapField] for [onUpdate]'s semantics. */
	fun itemListField(size: Int, onUpdate: (() -> Unit)? = null): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieStorageList<ArchieItemStorage>>>

	/** Declares an [ArchieFluidStorage] field with [size] tank slots each capped at [limit], keyed by the delegated property's name. See [itemField] for [onUpdate]'s semantics. */
	fun fluidField(limit: Long, size: Int = 1, onUpdate: (() -> Unit)? = null): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieFluidStorage>>

	/** Declares an [ArchieStorageMap] of [ArchieFluidStorage]s, each with [size] tank slots capped at [limit], keyed by the delegated property's name. See [itemMapField] for [onUpdate]'s semantics. */
	fun fluidMapField(limit: Long, size: Int = 1, onUpdate: (() -> Unit)? = null): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieStorageMap<ArchieFluidStorage>>>

	/** Declares an [ArchieStorageList] of [ArchieFluidStorage]s, each with [size] tank slots capped at [limit], keyed by the delegated property's name. See [itemMapField] for [onUpdate]'s semantics. */
	fun fluidListField(limit: Long, size: Int = 1, onUpdate: (() -> Unit)? = null): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieStorageList<ArchieFluidStorage>>>

	/** Declares an [ArchieEnergyStorage] field capped at [capacity], keyed by the delegated property's name. See [itemField] for [onUpdate]'s semantics. */
	fun energyField(capacity: Long, onUpdate: (() -> Unit)? = null): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieEnergyStorage>>

	/** Declares an [ArchieStorageMap] of [ArchieEnergyStorage]s, each capped at [capacity], keyed by the delegated property's name. See [itemMapField] for [onUpdate]'s semantics. */
	fun energyMapField(capacity: Long, onUpdate: (() -> Unit)? = null): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieStorageMap<ArchieEnergyStorage>>>

	/** Declares an [ArchieStorageList] of [ArchieEnergyStorage]s, each capped at [capacity], keyed by the delegated property's name. See [itemMapField] for [onUpdate]'s semantics. */
	fun energyListField(capacity: Long, onUpdate: (() -> Unit)? = null): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieStorageList<ArchieEnergyStorage>>>

	fun booleanField(default: () -> Boolean = { false }): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, Boolean>> = field(Boolean.serializer(), default)
	fun byteField(default: () -> Byte = { 0 }): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, Byte>> = field(Byte.serializer(), default)
	fun ubyteField(default: () -> UByte = { 0u }): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, UByte>> = field(UByte.serializer(), default)
	fun shortField(default: () -> Short = { 0 }): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, Short>> = field(Short.serializer(), default)
	fun ushortField(default: () -> UShort = { 0u }): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, UShort>> = field(UShort.serializer(), default)
	fun intField(default: () -> Int = { 0 }): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, Int>> = field(Int.serializer(), default)
	fun uintField(default: () -> UInt = { 0u }): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, UInt>> = field(UInt.serializer(), default)
	fun longField(default: () -> Long = { 0 }): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, Long>> = field(Long.serializer(), default)
	fun ulongField(default: () -> ULong = { 0u }): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, ULong>> = field(ULong.serializer(), default)
	fun floatField(default: () -> Float = { 0.0f }): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, Float>> = field(Float.serializer(), default)
	fun doubleField(default: () -> Double = { 0.0 }): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, Double>> = field(Double.serializer(), default)
	fun stringField(default: () -> String = { "" }): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, String>> = field(String.serializer(), default)

	/** Loads every declared field's value from [compoundTag], overwriting current values. */
	fun loadFromTag(compoundTag: CompoundTag)

	/** Writes every declared field's current value into [compoundTag]. */
	fun saveToTag(compoundTag: CompoundTag)

	/** Builds a [CompoundTag] suitable for sending to the client to sync current field values. */
	fun getSyncTag(): CompoundTag

	/** Updates a single field, identified by [propertyName], from a client sync payload. */
	fun <T> updateProperty(propertyName: String, serializer: KSerializer<T>, value: T)

	companion object
	{
		/** Creates a standalone [NBTHolder] not backed by any particular [ItemStack]/[FluidStack]. */
		fun create(): NBTHolder = NBTHolderImpl()

		/** Creates an [NBTHolder] whose fields are persisted to [stack]'s NBT. */
		fun item(stack: ItemStack): NBTHolder = ItemStackNBTHolderImpl(stack)

		/** Creates an [NBTHolder] for [stack] and immediately runs [block] against it. */
		fun <R> item(stack: ItemStack, block: NBTHolder.() -> R): R
		{
			return item(stack).block()
		}

		/** Creates an [NBTHolder] whose fields are persisted to [stack]'s NBT. */
		fun fluid(stack: FluidStack): NBTHolder = FluidStackNBTHolderImpl(stack)

		/** Creates an [NBTHolder] for [stack] and immediately runs [block] against it. */
		fun <R> fluid(stack: FluidStack, block: NBTHolder.() -> R): R
		{
			return fluid(stack).block()
		}
	}
}

/** Reified variant of [NBTHolder.field] that resolves the [KSerializer] for [T] automatically. */
inline fun <reified T> NBTHolder.field(noinline default: () -> T): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, T>> = field(serializer<T>(), default)
/** Reified variant of [NBTHolder.listField] that resolves the [KSerializer] for [T] automatically. */
inline fun <reified T> NBTHolder.listField(noinline default: () -> List<T>): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, MutableList<T>>> = listField(serializer<T>(), default)
/** Reified variant of [NBTHolder.mapField] that resolves the [KSerializer] for [T] automatically. */
inline fun <reified T> NBTHolder.mapField(noinline default: () -> Map<String, T>): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, MutableMap<String, T>>> = mapField(serializer<T>(), default)