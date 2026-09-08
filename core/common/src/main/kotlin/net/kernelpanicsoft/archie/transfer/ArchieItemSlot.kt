package net.kernelpanicsoft.archie.transfer

import net.kernelpanicsoft.archie.serialization.SerializationManager
import net.kernelpanicsoft.archie.serialization.decodeFromNbtTagRootless
import net.kernelpanicsoft.archie.serialization.encodeToNbtTagRootless
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
import net.kernelpanicsoft.archie.serialization.kSerializer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import java.util.function.Predicate
import kotlin.math.min

/**
 * A single resource-backed slot inside an [ArchieItemStorage]. Tracks an [ItemResource] +
 * amount internally (for Common Storage Lib's resource-based [insert]/[extract]) while exposing
 * plain [ItemStack] access via [getItem]/[set].
 *
 * @param onUpdate Invoked by [update] whenever this slot's contents should be persisted/synced.
 */
@Serializable(with = ArchieItemSlot.Serializer::class)
class ArchieItemSlot(private val filter: Predicate<ItemResource> = Predicate { true }, private val onUpdate: () -> Unit = {}) : StorageSlot<ItemResource>, UpdateManager<NbtTag>
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
			update()
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
			update()
		}

	constructor(stack: ItemStack = ItemStack.EMPTY, filter: Predicate<ItemResource> = Predicate { true }, onUpdate: () -> Unit = {}) : this(filter, onUpdate)
	{
		this.stack = stack
	}

	constructor(resourceStack: ResourceStack<ItemResource>, filter: Predicate<ItemResource> = Predicate { true }, onUpdate: () -> Unit = {}) : this(filter, onUpdate)
	{
		this.resourceStack = resourceStack
	}

	/** The [ItemStack] currently held in this slot (a copy; mutate via [set]). */
	fun getItem(): ItemStack = stack
	/** Replaces this slot's contents with [value]. */
	fun set(value: ItemStack)
	{
		stack = value
	}

	/** Splits up to [amount] items off this slot's stack and returns them, leaving the rest in place. */
	fun remove(amount: Int): ItemStack
	{
		return if (!stack.isEmpty && amount > 0) stack.let {
			val ret = it.split(amount)
			stack = it
			ret
		} else ItemStack.EMPTY
	}

	/** The maximum stack size for the resource currently held (or [Item.ABSOLUTE_MAX_STACK_SIZE] if empty). */
	fun getMaxStackSize(): Int = getLimit(resource).toInt()

	override fun insert(unit: ItemResource, amount: Long, simulate: Boolean): Long
	{
		if (!isResourceValid(unit)) return 0
		if (this.resource.isBlank)
		{
			val inserted =
				min(amount.toDouble(), unit.cachedStack.maxStackSize.toDouble()).toLong()
			if (!simulate && inserted > 0)
			{
				this.resource = unit
				this.amount = inserted
				update()
			}
			return inserted
		} else if (this.resource.test(unit.toStack()))
		{
			val inserted =
				min(amount.toDouble(), (getLimit(resource) - this.amount).toDouble()).toLong()
			if (!simulate && inserted > 0)
			{
				this.amount += inserted
				update()
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
			if (!simulate && extracted > 0)
			{
				this.amount -= extracted
				if (this.amount == 0L)
				{
					this.resource = ItemResource.BLANK
				}
				update()
			}
			return extracted
		}
		return 0
	}

	override fun getLimit(resource: ItemResource): Long =
		if (resource.isBlank) Item.ABSOLUTE_MAX_STACK_SIZE.toLong()
		else resource.cachedStack.maxStackSize.toLong()

	override fun isResourceValid(unit: ItemResource): Boolean = filter.test(unit)

	override fun getResource(): ItemResource = resource

	override fun getAmount(): Long = amount

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
		this.stack = SerializationManager.nbt.decodeFromNbtTagRootless(serializer(), snapshot).stack
	}

	/** Serializes an [ArchieItemSlot] as its underlying [ResourceStack], or `null` when blank. */
	object Serializer : KSerializer<ArchieItemSlot>
	{
		private val surrogate = ResourceStack.ITEM_CODEC.kSerializer
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