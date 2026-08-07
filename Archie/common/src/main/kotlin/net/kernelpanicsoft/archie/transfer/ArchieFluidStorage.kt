package net.kernelpanicsoft.archie.transfer

import earth.terrarium.common_storage_lib.resources.fluid.FluidResource
import earth.terrarium.common_storage_lib.storage.base.CommonStorage
import earth.terrarium.common_storage_lib.storage.base.UpdateManager
import earth.terrarium.common_storage_lib.storage.util.TransferUtil
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import net.benwoodworth.knbt.NbtTag
import net.kernelpanicsoft.archie.serialization.NBT
import net.kernelpanicsoft.archie.serialization.decodeFromNbtTagRootless
import net.kernelpanicsoft.archie.serialization.encodeToNbtTagRootless
import net.minecraft.core.NonNullList
import kotlin.math.min

/**
 * The fluid analogue of [ArchieItemStorage]: a fixed-size list of [ArchieFluidSlot]s, each
 * capped at [limit], implementing Common Storage Lib's [CommonStorage] and Archie's NBT
 * serialization (via [Serializer]) for save/load.
 *
 * @param onUpdate Invoked by [update] whenever the storage's contents should be persisted/synced.
 */
@Serializable(with = ArchieFluidStorage.Serializer::class)
open class ArchieFluidStorage private constructor(
	protected val limit: Long,
	protected val slots: NonNullList<ArchieFluidSlot>,
	protected val onUpdate: () -> Unit = {}
) : CommonStorage<FluidResource>, UpdateManager<NbtTag>
{
	/** Creates a storage with [size] empty slots, each capped at [limit]. */
	constructor(limit: Long, size: Int, onUpdate: () -> Unit = {}) : this(
		limit,
		NonNullList.createWithCapacity<ArchieFluidSlot>(size).apply {
			for (i in 0 until size)
			{
				add(ArchieFluidSlot(limit))
			}
		}, onUpdate
	)

	override fun insert(unit: FluidResource, amount: Long, simulate: Boolean): Long
	{
		return TransferUtil.insertSlots(this, unit, amount, simulate)
	}

	override fun extract(unit: FluidResource, amount: Long, simulate: Boolean): Long
	{
		return TransferUtil.extractSlots(this, unit, amount, simulate)
	}

	/** The number of slots in this storage. */
	override fun size(): Int = slots.size

	/** The [ArchieFluidSlot] at [slot]. */
	override fun get(slot: Int): ArchieFluidSlot
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

	/** Serializes an [ArchieFluidStorage] as its [limit] followed by the list of its [ArchieFluidSlot]s. */
	object Serializer : KSerializer<ArchieFluidStorage>
	{
		private val surrogate = ListSerializer(ArchieFluidSlot.serializer())
		override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ArchieFluidStorage") {
			element("limit", Long.serializer().descriptor)
			element("slots", surrogate.descriptor)
		}

		override fun deserialize(decoder: Decoder): ArchieFluidStorage
		{
			return decoder.decodeStructure(descriptor)
			{
				var limit = 0L
				var slots: List<ArchieFluidSlot> = emptyList()
				while (true)
				{
					when (val index = decodeElementIndex(descriptor))
					{
						0 -> limit = decodeLongElement(descriptor, 0)
						1 -> slots = decodeSerializableElement(descriptor, 1, surrogate)
						CompositeDecoder.DECODE_DONE -> break
						else -> error("Unexpected index: $index")
					}
				}
				ArchieFluidStorage(limit, NonNullList.of(ArchieFluidSlot(limit), *slots.toTypedArray()))
			}
		}

		override fun serialize(encoder: Encoder, value: ArchieFluidStorage)
		{
			encoder.encodeStructure(descriptor)
			{
				encodeLongElement(descriptor, 0, value.limit)
				encodeSerializableElement(descriptor, 1, surrogate, value.slots)
			}
		}
	}
}