package net.kernelpanicsoft.archie.serialization

import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.buildNbtCompound
import net.minecraft.nbt.CompoundTag

/**
 * A mutable map of nested [NBTHolder]s, keyed by [String], declared via [NBTHolder.nestedMapField].
 * Each entry is itself a self-contained, independently `field()`-declared [NBTHolder] rather than
 * one shared value shape - useful when a map's entries are heterogeneous (e.g. one holder type per
 * registry-driven "kind" of thing an entry represents), which [NBTHolder.mapField] can't express
 * since it needs one fixed, kotlinx.serialization-compatible value type for every entry.
 *
 * Structural changes ([getOrPut], [remove]) mark the owning holder dirty/resynced automatically.
 * Mutating an *existing* entry's own field in place does not - call [touch] afterward.
 */
class NestedNBTHolderMap internal constructor(private val onChange: () -> Unit) : Iterable<Map.Entry<String, NBTHolder>> {
	private val entries: MutableMap<String, NBTHolder> = mutableMapOf()

	val keys: Set<String> get() = entries.keys
	val size: Int get() = entries.size

	operator fun get(key: String): NBTHolder? = entries[key]
	fun containsKey(key: String): Boolean = key in entries

	/** Returns the existing nested holder at [key], or builds one via [factory], stores it, and marks this dirty. */
	fun <T : NBTHolder> getOrPut(key: String, factory: () -> T): T {
		val existing = entries[key]
		if (existing != null) {
			@Suppress("UNCHECKED_CAST")
			return existing as T
		}
		val created = factory()
		entries[key] = created
		onChange()
		return created
	}

	/** Removes and returns the nested holder at [key], marking this dirty - `null` if it wasn't present. */
	fun remove(key: String): NBTHolder? {
		val removed = entries.remove(key) ?: return null
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

	override fun iterator(): Iterator<Map.Entry<String, NBTHolder>> = entries.entries.iterator()

	/** Rebuilds every entry from [compound] via [factory], discarding current contents. Does not call [onChange]. */
	internal fun loadFrom(compound: NbtCompound?, factory: (CompoundTag) -> NBTHolder) {
		entries.clear()
		compound?.forEach { (key, tag) ->
			val subTag = (tag as? NbtCompound)?.toMinecraft ?: return@forEach
			entries[key] = factory(subTag).also { it.loadFromTag(subTag) }
		}
	}

	/** Serializes every entry to its own sub-[CompoundTag], keyed by its map key. */
	internal fun toNbtCompound(): NbtCompound = buildNbtCompound {
		for ((key, holder) in entries) {
			val subTag = CompoundTag()
			holder.saveToTag(subTag)
			put(key, subTag.fromMinecraft)
		}
	}
}
