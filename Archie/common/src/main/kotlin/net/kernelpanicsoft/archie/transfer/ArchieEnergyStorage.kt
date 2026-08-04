package net.kernelpanicsoft.archie.transfer

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
 * Archie's platform-agnostic energy buffer: a single [capacity]-capped [Long] amount, with
 * resource-style [insert]/[extract] mirroring [ArchieItemStorage]/[ArchieFluidStorage].
 *
 * Unlike items and fluids, there is no single cross-loader energy standard - Archie doesn't
 * bridge this to a platform capability (NeoForge's `IEnergyStorage`, Fabric's Team Reborn Energy
 * API) for you. Wire that yourself in loader-specific code, reading/writing through [getAmount],
 * [getCapacity], [insert], and [extract]; the [net.kernelpanicsoft.archie.gui.composables.basic.EnergyBar]
 * composable only needs the plain amount/capacity pair to render.
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
)
{
	private var amount: Long = 0

	/** The amount of energy currently stored. */
	fun getAmount(): Long = amount

	/** The maximum amount of energy this storage can hold. */
	fun getCapacity(): Long = capacity

	/**
	 * Inserts up to [amount] energy, returning how much was actually accepted.
	 * When [simulate] is `true`, no state is changed - only the acceptable amount is calculated.
	 */
	fun insert(amount: Long, simulate: Boolean): Long
	{
		val inserted = min(amount, capacity - this.amount)
		if (inserted <= 0) return 0
		if (!simulate)
		{
			this.amount += inserted
			onUpdate()
		}
		return inserted
	}

	/**
	 * Extracts up to [amount] energy, returning how much was actually removed.
	 * When [simulate] is `true`, no state is changed - only the extractable amount is calculated.
	 */
	fun extract(amount: Long, simulate: Boolean): Long
	{
		val extracted = min(amount, this.amount)
		if (extracted <= 0) return 0
		if (!simulate)
		{
			this.amount -= extracted
			onUpdate()
		}
		return extracted
	}

	/** Directly overwrites the stored amount, clamped to `0..`[capacity]. */
	fun set(amount: Long)
	{
		this.amount = amount.coerceIn(0, capacity)
		onUpdate()
	}

	/** Snapshots this storage's [capacity] and stored amount as an [NbtTag], for save/sync. */
	fun createSnapshot(): NbtTag = NBT.encodeToNbtTagRootless(serializer(), this)

	/** Restores this storage's [capacity] and stored amount from a snapshot produced by [createSnapshot]. */
	fun readSnapshot(snapshot: NbtTag)
	{
		val decoded = NBT.decodeFromNbtTagRootless(serializer(), snapshot)
		this.capacity = decoded.capacity
		this.amount = decoded.amount
	}

	/** Serializes an [ArchieEnergyStorage] as its [capacity] followed by its stored amount. */
	object Serializer : KSerializer<ArchieEnergyStorage>
	{
		override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ArchieEnergyStorage") {
			element("capacity", Long.serializer().descriptor)
			element("amount", Long.serializer().descriptor)
		}

		override fun deserialize(decoder: Decoder): ArchieEnergyStorage
		{
			val capacity = decoder.decodeLong()
			val amount = decoder.decodeLong()
			return ArchieEnergyStorage(capacity).also { it.amount = amount }
		}

		override fun serialize(encoder: Encoder, value: ArchieEnergyStorage)
		{
			encoder.encodeLong(value.capacity)
			encoder.encodeLong(value.amount)
		}
	}
}
