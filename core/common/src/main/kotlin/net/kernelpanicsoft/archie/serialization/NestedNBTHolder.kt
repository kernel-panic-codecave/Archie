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

	internal fun toNbtCompound(): NbtCompound
	{
		val tag = CompoundTag()
		value?.saveToTag(tag)
		return tag.fromMinecraft
	}
}