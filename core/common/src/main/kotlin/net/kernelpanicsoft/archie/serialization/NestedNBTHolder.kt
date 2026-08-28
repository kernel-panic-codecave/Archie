package net.kernelpanicsoft.archie.serialization

import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.buildNbtCompound
import net.minecraft.nbt.CompoundTag
import kotlin.text.toIntOrNull

class NestedNBTHolder<T : NBTHolder> internal constructor(private val onChange: () -> Unit)
{
	private var holder: T? = null

	var value: T?
		get() = holder
		set(value) {
			holder = value
			onChange()
		}

	/** Marks this dirty without a structural change - call after mutating an existing entry's own field(s) in place. */
	fun touch() = onChange()

	@Suppress("UNCHECKED_CAST")
	internal fun loadFrom(compound: NbtCompound?, factory: (CompoundTag) -> NBTHolder?) {
		holder = null
		compound ?: return
		val tag = compound.toMinecraft
		((factory(tag)?.also { it.loadFromTag(tag) }) as T?)?.let { holder = it }
	}

	internal fun toNbtCompound(): NbtCompound
	{
		val tag = CompoundTag()
		holder?.saveToTag(tag)
		return tag.fromMinecraft
	}
}