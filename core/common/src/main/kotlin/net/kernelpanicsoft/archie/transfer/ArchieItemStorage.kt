package net.kernelpanicsoft.archie.transfer

import net.kernelpanicsoft.archie.serialization.SerializationManager
import net.kernelpanicsoft.archie.serialization.decodeFromNbtTagRootless
import net.kernelpanicsoft.archie.serialization.encodeToNbtTagRootless
import earth.terrarium.common_storage_lib.resources.item.ItemResource
import earth.terrarium.common_storage_lib.storage.base.CommonStorage
import earth.terrarium.common_storage_lib.storage.base.UpdateManager
import earth.terrarium.common_storage_lib.storage.util.TransferUtil
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.benwoodworth.knbt.NbtTag
import net.minecraft.core.NonNullList
import java.util.function.Predicate
import kotlin.math.min

/**
 * Archie's platform-agnostic item container: a fixed-size list of [ArchieItemSlot]s that
 * implements Common Storage Lib's [CommonStorage] (resource-based [insert]/[extract] for
 * capability interop) and Archie's NBT serialization (via [Serializer]) for save/load.
 *
 * Usually created through [net.kernelpanicsoft.archie.serialization.NBTHolder.itemField] rather
 * than directly. Its public surface is deliberately small; read or mutate the [net.minecraft.world.item.ItemStack] in a
 * slot through the [ArchieItemSlot] returned by [get], not on the storage itself.
 *
 * @param onUpdate Invoked by [update] whenever the storage's contents should be persisted/synced.
 */
@Serializable(with = ArchieItemStorage.Serializer::class)
open class ArchieItemStorage private constructor(
	protected var slots: NonNullList<ArchieItemSlot>,
	protected val filter: Predicate<ItemResource> = Predicate { true },
	protected val onUpdate: () -> Unit = {}
) : CommonStorage<ItemResource>, UpdateManager<NbtTag>
{
	/** Creates a storage with [size] empty slots. */
	constructor(size: Int, filter: Predicate<ItemResource> = Predicate { true }, onUpdate: () -> Unit = {}) : this(
		NonNullList.createWithCapacity<ArchieItemSlot>(size).apply {
			for (i in 0 until size)
			{
				add(ArchieItemSlot(filter))
			}
		}, filter, onUpdate
	)

	override fun insert(unit: ItemResource, amount: Long, simulate: Boolean): Long
	{
		return TransferUtil.insertSlots(this, unit, amount, simulate)
	}

	override fun extract(unit: ItemResource, amount: Long, simulate: Boolean): Long
	{
		return TransferUtil.extractSlots(this, unit, amount, simulate)
	}

	/** The number of slots in this storage. */
	override fun size(): Int = slots.size

	/** The [ArchieItemSlot] at [slot], for reading/mutating its [net.minecraft.world.item.ItemStack]. */
	override fun get(slot: Int): ArchieItemSlot
	{
		return slots[slot]
	}

	override fun createSnapshot(): NbtTag
	{
		return SerializationManager.nbt.encodeToNbtTagRootless(serializer(), this)
	}

	override fun update()
	{
		onUpdate()
	}

	override fun readSnapshot(snapshot: NbtTag)
	{
		val slots = SerializationManager.nbt.decodeFromNbtTagRootless(serializer(), snapshot).slots
		for (i in 0 until min(this.slots.size, slots.size))
		{
			this.slots[i] = slots[i]
		}
	}

	/** Serializes an [ArchieItemStorage] as the plain list of its [ArchieItemSlot]s. */
	object Serializer : KSerializer<ArchieItemStorage>
	{
		private val surrogate = ListSerializer(ArchieItemSlot.serializer())
		override val descriptor: SerialDescriptor = surrogate.descriptor

		override fun deserialize(decoder: Decoder): ArchieItemStorage
		{
			return ArchieItemStorage(NonNullList.of(ArchieItemSlot(), *surrogate.deserialize(decoder).toTypedArray()))
		}

		override fun serialize(encoder: Encoder, value: ArchieItemStorage)
		{
			surrogate.serialize(encoder, value.slots)
		}

	}
}