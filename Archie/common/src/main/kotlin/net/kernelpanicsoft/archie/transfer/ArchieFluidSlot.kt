package net.kernelpanicsoft.archie.transfer

import dev.architectury.fluid.FluidStack
import earth.terrarium.common_storage_lib.resources.ResourceStack
import earth.terrarium.common_storage_lib.resources.fluid.FluidResource
import earth.terrarium.common_storage_lib.storage.base.StorageSlot
import earth.terrarium.common_storage_lib.storage.base.UpdateManager
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.benwoodworth.knbt.NbtTag
import net.kernelpanicsoft.archie.serialization.NBT
import net.kernelpanicsoft.archie.serialization.decodeFromNbtTagRootless
import net.kernelpanicsoft.archie.serialization.encodeToNbtTagRootless
import net.kernelpanicsoft.archie.serialization.kSerializer
import net.minecraft.world.item.Item
import kotlin.math.min

@Serializable(with = ArchieFluidSlot.Serializer::class)
class ArchieFluidSlot(private val limit: Long, private val onUpdate: () -> Unit = {}) : StorageSlot<FluidResource>, UpdateManager<NbtTag>
{
	private var resource: FluidResource = FluidResource.BLANK
	private var amount: Long = 0
	private var stack: FluidStack
		get()
		{
			if (resource.isBlank)
				return FluidStack.empty()
			return FluidStack.create(resource.type, amount)
		}
		set(value)
		{
			resource = FluidResource.of(value.fluid)
			amount = value.amount
		}

	private var resourceStack: ResourceStack<FluidResource>
		get()
		{
			return ResourceStack(resource, amount)
		}
		set(value)
		{
			resource = value.resource
			amount = value.amount
		}

	constructor(limit: Long, stack: FluidStack = FluidStack.empty(), onUpdate: () -> Unit = {}) : this(limit, onUpdate)
	{
		this.stack = stack
	}

	constructor(limit: Long, resourceStack: ResourceStack<FluidResource>, onUpdate: () -> Unit = {}) : this(limit, onUpdate)
	{
		this.resourceStack = resourceStack
	}

	fun getFluid(): FluidStack = stack
	fun set(value: FluidStack)
	{
		stack = value
	}



	override fun insert(unit: FluidResource, amount: Long, simulate: Boolean): Long
	{
		if (!isResourceValid(resource)) return 0
		if (this.resource.isBlank())
		{
			val inserted = min(amount, limit)
			if (!simulate)
			{
				this.resource = unit
				this.amount = inserted
			}
			return inserted
		} else if (this.resource == unit)
		{
			val inserted = min(amount, limit - this.amount)
			if (!simulate)
			{
				this.amount += inserted
			}
			return inserted
		}
		return 0
	}

	override fun extract(unit: FluidResource, amount: Long, simulate: Boolean): Long
	{
		if (this.resource == unit)
		{
			val extracted = min(amount, this.amount)
			if (!simulate)
			{
				this.amount -= extracted
				if (this.amount == 0L)
				{
					this.resource = FluidResource.BLANK
				}
			}
			return extracted
		}
		return 0
	}

	override fun getLimit(resource: FluidResource): Long = limit

	override fun isResourceValid(unit: FluidResource): Boolean = true

	override fun getResource(): FluidResource = resource

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

	object Serializer : KSerializer<ArchieFluidSlot>
	{
		private val surrogate = ResourceStack.FLUID_CODEC.kSerializer
		override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ArchieFluidSlot") {
			element("limit", Long.serializer().descriptor)
			element("resourceStack", surrogate.descriptor.nullable)
		}

		override fun deserialize(decoder: Decoder): ArchieFluidSlot
		{
			return ArchieFluidSlot(decoder.decodeLong(), surrogate.deserialize(decoder))
		}

		override fun serialize(
			encoder: Encoder,
			value: ArchieFluidSlot
		)
		{
			encoder.encodeLong(value.limit)
			surrogate.serialize(encoder, value.resourceStack)
		}
	}
}