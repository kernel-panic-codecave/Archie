package net.kernelpanicsoft.archie.serialization

import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.buildNbtCompound
import net.minecraft.nbt.CompoundTag

/**
 * A dynamically-sized, index-ordered list of nested [NBTHolder]s, declared via
 * [NBTHolder.nestedListField]. Like [NestedNBTHolderMap], entries can be heterogeneous - [add]
 * takes its own [factory], and reconstructing a saved entry inspects that entry's own raw
 * sub-[CompoundTag] first (the same [factory] shape [NBTHolder.nestedListField] itself takes) to
 * decide which concrete holder type to rebuild. [T] is the common upper bound every entry shares -
 * see [NestedNBTHolderMap]'s KDoc for why that's still useful even for a genuinely heterogeneous
 * list.
 *
 * Persisted as an [NbtCompound] keyed by stringified index, not a genuine NBT list - see
 * [ArchieStorageList]'s KDoc for why (the same reasoning applies: every entry is its own
 * sub-[CompoundTag], but a raw NBT `ListTag` requires one uniform concrete tag type, which knbt
 * only exposes a public builder API for when that's statically known).
 */
class NestedNBTHolderList<T : NBTHolder> internal constructor(private val onChange: () -> Unit) : Iterable<T> {
	private val entries: MutableList<T> = mutableListOf()

	val size: Int get() = entries.size
	operator fun get(index: Int): T = entries[index]

	/** Builds a new entry via [factory], appends it, and marks this dirty. */
	fun <R : T> add(factory: () -> R): R {
		val created = factory()
		entries += created
		onChange()
		return created
	}

	/** Removes and returns the entry at [index], marking this dirty. */
	fun removeAt(index: Int): T {
		val removed = entries.removeAt(index)
		onChange()
		return removed
	}

	fun clear() {
		if (entries.isEmpty()) return
		entries.clear()
		onChange()
	}

	/** Marks this dirty without a structural change - call after mutating an existing entry's own field(s) in place. */
	fun touch() = onChange()

	override fun iterator(): Iterator<T> = entries.iterator()

	/**
	 * Rebuilds every entry, in index order, from [compound]'s stringified-index keys via [factory].
	 * Does not call [onChange]. See [NestedNBTHolderMap.loadFrom]'s KDoc for why [factory] takes
	 * plain [NBTHolder] rather than [T].
	 */
	@Suppress("UNCHECKED_CAST")
	internal fun loadFrom(compound: NbtCompound?, factory: (CompoundTag) -> NBTHolder) {
		entries.clear()
		compound ?: return
		compound.keys.mapNotNull { it.toIntOrNull() }.sorted().forEach { index ->
			val tag = compound.getValue(index.toString())
			val subTag = (tag as? NbtCompound)?.toMinecraft ?: return@forEach
			entries += (factory(subTag).also { it.loadFromTag(subTag) }) as T
		}
	}

	/** Serializes every entry to its own sub-[CompoundTag], under its (reassigned, 0-based) index as a string key. */
	internal fun toNbtCompound(): NbtCompound = buildNbtCompound {
		entries.forEachIndexed { index, holder ->
			val subTag = CompoundTag()
			holder.saveToTag(subTag)
			put(index.toString(), subTag.fromMinecraft)
		}
	}
}
