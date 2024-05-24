package com.withertech.archie.transfer

import com.withertech.archie.Archie
import com.withertech.archie.serialization.NBT
import com.withertech.archie.serialization.decodeFromNbtTagRootless
import com.withertech.archie.serialization.encodeToNbtTagRootless
import earth.terrarium.botarium.item.impl.SimpleItemStorage
import earth.terrarium.botarium.resources.item.ItemResource
import earth.terrarium.botarium.storage.base.CommonStorage
import earth.terrarium.botarium.storage.base.UpdateManager
import earth.terrarium.botarium.storage.util.TransferUtil
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ArraySerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.benwoodworth.knbt.NbtTag
import net.minecraft.core.NonNullList
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max
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

	override fun getSlotCount(): Int = slots.size

	override fun getSlot(slot: Int): ArchieItemSlot
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