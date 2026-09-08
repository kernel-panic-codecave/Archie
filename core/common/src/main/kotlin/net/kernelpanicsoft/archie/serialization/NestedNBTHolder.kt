package net.kernelpanicsoft.archie.serialization

import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.buildNbtCompound
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.codec.ByteBufCodecs.holder
import kotlin.properties.Delegates
import kotlin.text.toIntOrNull

class NestedNBTHolder<T : NBTHolder> internal constructor(private val onChange: () -> Unit)
{
	var value: T? by Delegates.observable(null) { _, _, _ -> onChange() }

	fun getOrSet(getter: () -> T): T = value ?: getter().also { value = it }

	/** Marks this dirty without a structural change - call after mutating an existing entry's own field(s) in place. */
	fun touch() = onChange()

	@Suppress("UNCHECKED_CAST")
	internal fun loadFrom(compound: NbtCompound?, factory: (CompoundTag) -> NBTHolder?) {
		value = null
		compound ?: return
		val tag = compound.toMinecraft
		((factory(tag)?.also { it.loadFromTag(tag) }) as T?)?.let { value = it }
	}

	/**
	 * This holder's contents, or `null` when it holds nothing.
	 *
	 * Nullable rather than an empty compound, and the owning holder removes the key entirely for a
	 * `null` - otherwise an *unset* nested field saves as `{}`, and [loadFrom] cannot tell that
	 * apart from a holder that genuinely serialized to nothing. It would hand the empty tag to the
	 * factory, which either resurrects a default-constructed instance where there had been none, or
	 * throws: a factory reads its discriminator out of that tag (`tag.getString("type")`), and there
	 * is nothing there to read. Every factory in the wild has had to defend against being called
	 * this way, one of them by switching to `tryParse` after being bitten. Absence now round-trips
	 * as absence, so it never gets called at all.
	 */
	internal fun toNbtCompoundOrNull(): NbtCompound?
	{
		val held = value ?: return null
		val tag = CompoundTag()
		held.saveToTag(tag)
		return tag.fromMinecraft
	}
}