package net.kernelpanicsoft.archie.transfer

import earth.terrarium.common_storage_lib.storage.base.UpdateManager
import earth.terrarium.common_storage_lib.storage.base.ValueStorage
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.benwoodworth.knbt.NbtTag
import net.kernelpanicsoft.archie.serialization.NBT
import net.kernelpanicsoft.archie.serialization.decodeFromNbtTagRootless
import net.kernelpanicsoft.archie.serialization.encodeToNbtTagRootless
import kotlin.math.min

/**
 * Archie's platform-agnostic energy buffer: implements Common Storage Lib's [ValueStorage] -
 * the energy analogue of the `CommonStorage<ItemResource>`/`CommonStorage<FluidResource>`
 * [ArchieItemStorage]/[ArchieFluidStorage] implement - plus Archie's NBT serialization for
 * save/load, mirroring their shape.
 *
 * Archie doesn't register this with Common Storage Lib's `EnergyApi.BLOCK`/`ITEM`/`ENTITY`
 * lookups for you, the same way it doesn't for `ArchieItemStorage`/`ArchieFluidStorage` today -
 * wire that lookup registration, and any platform-specific capability bridge (NeoForge's
 * `IEnergyStorage`, Fabric's Team Reborn Energy API) you still want on top of it, in your own mod.
 *
 * Usually created through [net.kernelpanicsoft.archie.serialization.NBTHolder.energyField]
 * rather than directly.
 *
 * @param capacity The maximum amount of energy this storage can hold.
 * @param onUpdate Invoked whenever this storage's contents change, for persistence/sync.
 */
@Serializable(with = ArchieEnergyStorage.Serializer::class)
class ArchieEnergyStorage(
	private var capacity: Long,
	private val onUpdate: () -> Unit = {},
) : ValueStorage, UpdateManager<NbtTag>
{
	private var amount: Long = 0

	/** The amount of energy currently stored. */
	override fun getStoredAmount(): Long = amount

	/** The maximum amount of energy this storage can hold. */
	override fun getCapacity(): Long = capacity

	override fun allowsInsertion(): Boolean = true

	override fun allowsExtraction(): Boolean = true

	/**
	 * Inserts up to [amount] energy, returning how much was actually accepted.
	 * When [simulate] is `true`, no state is changed - only the acceptable amount is calculated.
	 */
	override fun insert(amount: Long, simulate: Boolean): Long
	{
		val inserted = min(amount, capacity - this.amount)
		if (inserted <= 0) return 0
		if (!simulate)
		{
			this.amount += inserted
			update()
		}
		return inserted
	}

	/**
	 * Extracts up to [amount] energy, returning how much was actually removed.
	 * When [simulate] is `true`, no state is changed - only the extractable amount is calculated.
	 */
	override fun extract(amount: Long, simulate: Boolean): Long
	{
		val extracted = min(amount, this.amount)
		if (extracted <= 0) return 0
		if (!simulate)
		{
			this.amount -= extracted
			update()
		}
		return extracted
	}

	/** Directly overwrites the stored amount, clamped to `0..`[getCapacity]. */
	fun set(amount: Long)
	{
		this.amount = amount.coerceIn(0, capacity)
		update()
	}

	/** Snapshots this storage's [getCapacity] and stored amount as an [NbtTag], for save/sync. */
	override fun createSnapshot(): NbtTag = NBT.encodeToNbtTagRootless(serializer(), this)

	/**
	 * Restores this storage's capacity and stored amount from a snapshot produced by
	 * [createSnapshot]. `capacity` is clamped to non-negative and `amount` to `0..capacity` - a
	 * malformed or stale snapshot (e.g. from before a capacity change) shouldn't be able to leave
	 * this storage over-capacity or negative, which would otherwise wedge [insert]/[extract].
	 */
	override fun readSnapshot(snapshot: NbtTag)
	{
		val decoded = NBT.decodeFromNbtTagRootless(serializer(), snapshot)
		this.capacity = decoded.capacity.coerceAtLeast(0)
		this.amount = decoded.amount.coerceIn(0, this.capacity)
	}

	override fun update() = onUpdate()

	/** Serializes an [ArchieEnergyStorage] as its capacity followed by its stored amount. */
	object Serializer : KSerializer<ArchieEnergyStorage>
	{
		override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ArchieEnergyStorage") {
			element("capacity", Long.serializer().descriptor)
			element("amount", Long.serializer().descriptor)
		}

		override fun deserialize(decoder: Decoder): ArchieEnergyStorage
		{
			val capacity = decoder.decodeLong().coerceAtLeast(0)
			val amount = decoder.decodeLong().coerceIn(0, capacity)
			return ArchieEnergyStorage(capacity).also { it.amount = amount }
		}

		override fun serialize(encoder: Encoder, value: ArchieEnergyStorage)
		{
			encoder.encodeLong(value.capacity)
			encoder.encodeLong(value.amount)
		}
	}
}
