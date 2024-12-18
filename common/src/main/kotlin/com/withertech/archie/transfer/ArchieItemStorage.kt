package com.withertech.archie.transfer

import com.withertech.archie.serialization.NBT
import com.withertech.archie.serialization.decodeFromNbtTagRootless
import com.withertech.archie.serialization.encodeToNbtTagRootless
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
import kotlin.math.min

@Serializable(with = ArchieItemStorage.Serializer::class)
open class ArchieItemStorage private constructor(
	protected var slots: NonNullList<ArchieItemSlot>,
	protected val onUpdate: () -> Unit = {}
) : CommonStorage<ItemResource>, UpdateManager<NbtTag>
{
	constructor(size: Int, onUpdate: () -> Unit = {}) : this(
		NonNullList.createWithCapacity<ArchieItemSlot>(size).apply {
			for (i in 0 until size)
			{
				add(ArchieItemSlot())
			}
		}, onUpdate
	)

	override fun insert(unit: ItemResource, amount: Long, simulate: Boolean): Long
	{
		return TransferUtil.insertSlots(this, unit, amount, simulate)
	}

	override fun extract(unit: ItemResource, amount: Long, simulate: Boolean): Long
	{
		return TransferUtil.extractSlots(this, unit, amount, simulate)
	}

	override fun size(): Int = slots.size

	override fun get(slot: Int): ArchieItemSlot
	{
		return slots[slot]
	}

	override fun createSnapshot(): NbtTag
	{
		return NBT.encodeToNbtTagRootless(serializer(), this)
	}

	override fun update()
	{
		onUpdate()
	}

	override fun readSnapshot(snapshot: NbtTag)
	{
		val slots = NBT.decodeFromNbtTagRootless(serializer(), snapshot).slots
		for (i in 0 until min(this.slots.size, slots.size))
		{
			this.slots[i] = slots[i]
		}
	}

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