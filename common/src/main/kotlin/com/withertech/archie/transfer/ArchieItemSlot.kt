package com.withertech.archie.transfer

import com.withertech.archie.serialization.NBT
import com.withertech.archie.serialization.decodeFromNbtTagRootless
import com.withertech.archie.serialization.encodeToNbtTagRootless
import com.withertech.archie.serialization.serializer
import earth.terrarium.common_storage_lib.resources.ResourceStack
import earth.terrarium.common_storage_lib.resources.item.ItemResource
import earth.terrarium.common_storage_lib.storage.base.StorageSlot
import earth.terrarium.common_storage_lib.storage.base.UpdateManager
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.benwoodworth.knbt.NbtTag
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import kotlin.math.min

@Serializable(with = ArchieItemSlot.Serializer::class)
class ArchieItemSlot(private val onUpdate: () -> Unit = {}) : StorageSlot<ItemResource>, UpdateManager<NbtTag>
{
	private var resource: ItemResource = ItemResource.BLANK
	private var amount: Long = 0
	private var stack: ItemStack
		get()
		{
			if (resource.isBlank)
				return ItemStack.EMPTY
			return resource.toStack(amount.toInt())
		}
		set(value)
		{
			resource = ItemResource.of(value)
			amount = value.count.toLong()
		}

	private var resourceStack: ResourceStack<ItemResource>
		get()
		{
			return ResourceStack(resource, amount)
		}
		set(value)
		{
			resource = value.resource
			amount = value.amount
		}

	constructor(stack: ItemStack = ItemStack.EMPTY, onUpdate: () -> Unit = {}) : this(onUpdate)
	{
		this.stack = stack
	}

	constructor(resourceStack: ResourceStack<ItemResource>, onUpdate: () -> Unit = {}) : this(onUpdate)
	{
		this.resourceStack = resourceStack
	}

	fun getItem(): ItemStack = stack
	fun set(value: ItemStack)
	{
		stack = value
	}

	fun remove(amount: Int): ItemStack
	{
		return if (!stack.isEmpty && amount > 0) stack.let {
			val ret = it.split(amount)
			stack = it
			ret
		} else ItemStack.EMPTY
	}

	fun getMaxStackSize(): Int = getLimit(resource).toInt()



	override fun insert(unit: ItemResource, amount: Long, simulate: Boolean): Long
	{
		if (!isResourceValid(unit)) return 0
		if (this.resource.isBlank)
		{
			val inserted =
				min(amount.toDouble(), unit.cachedStack.maxStackSize.toDouble()).toLong()
			if (!simulate)
			{
				this.resource = unit
				this.amount = inserted
			}
			return inserted
		} else if (this.resource.test(unit.toStack()))
		{
			val inserted =
				min(amount.toDouble(), (getLimit(resource) - this.amount).toDouble()).toLong()
			if (!simulate)
			{
				this.amount += inserted
			}
			return inserted
		}
		return 0
	}

	override fun extract(unit: ItemResource, amount: Long, simulate: Boolean): Long
	{
		if (this.resource.test(unit.toStack()))
		{
			val extracted = min(amount.toDouble(), this.amount.toDouble()).toLong()
			if (!simulate)
			{
				this.amount -= extracted
				if (this.amount == 0L)
				{
					this.resource = ItemResource.BLANK
				}
			}
			return extracted
		}
		return 0
	}

	override fun getLimit(resource: ItemResource): Long =
		if (resource.isBlank) Item.ABSOLUTE_MAX_STACK_SIZE.toLong()
		else resource.cachedStack.maxStackSize.toLong()

	override fun isResourceValid(unit: ItemResource): Boolean = true

	override fun getResource(): ItemResource = resource

	override fun getAmount(): Long = amount

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
		this.stack = NBT.decodeFromNbtTagRootless(serializer(), snapshot).stack
	}

	object Serializer : KSerializer<ArchieItemSlot>
	{
		private val surrogate = ResourceStack.ITEM_CODEC.serializer()
		override val descriptor: SerialDescriptor = surrogate.descriptor.nullable
		override fun deserialize(decoder: Decoder): ArchieItemSlot
		{
			return ArchieItemSlot(surrogate.deserialize(decoder))
		}

		override fun serialize(encoder: Encoder, value: ArchieItemSlot)
		{
			surrogate.serialize(encoder, value.resourceStack)
		}
	}
}