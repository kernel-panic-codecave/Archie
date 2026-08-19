package net.kernelpanicsoft.archie.serialization

import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.buildNbtCompound
import net.minecraft.nbt.CompoundTag

/**
 * A mutable map of nested [NBTHolder]s, keyed by [String], declared via [NBTHolder.nestedMapField].
 * Each entry is itself a self-contained, independently `field()`-declared [NBTHolder] rather than
 * one shared value shape - useful when a map's entries are heterogeneous (e.g. one holder type per
 * registry-driven "kind" of thing an entry represents), which [NBTHolder.mapField] can't express
 * since it needs one fixed, kotlinx.serialization-compatible value type for every entry. [T] is the
 * common upper bound every entry shares - plain [NBTHolder] itself if entries have nothing more
 * specific in common, or a narrower shared supertype (a caller can still insert any subtype of [T]
 * via [getOrPut]'s own type parameter) - so callers get correctly-typed entries back without a
 * manual cast at every read site.
 *
 * Structural changes ([getOrPut], [remove]) mark the owning holder dirty/resynced automatically.
 * Mutating an *existing* entry's own field in place does not - call [touch] afterward.
 */
class NestedNBTHolderMap<T : NBTHolder> internal constructor(private val onChange: () -> Unit) : Iterable<Map.Entry<String, T>> {
	private val entries: MutableMap<String, T> = mutableMapOf()

	val keys: Set<String> get() = entries.keys
	val size: Int get() = entries.size

	operator fun get(key: String): T? = entries[key]
	fun containsKey(key: String): Boolean = key in entries

	/** Returns the existing nested holder at [key], or builds one via [factory], stores it, and marks this dirty. */
	fun <R : T> getOrPut(key: String, factory: () -> R): R {
		val existing = entries[key]
		if (existing != null) {
			@Suppress("UNCHECKED_CAST")
			return existing as R
		}
		val created = factory()
		entries[key] = created
		onChange()
		return created
	}

	/** Removes and returns the nested holder at [key], marking this dirty - `null` if it wasn't present. */
	fun remove(key: String): T? {
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

	override fun iterator(): Iterator<Map.Entry<String, T>> = entries.entries.iterator()

	/**
	 * Rebuilds every entry from [compound] via [factory], discarding current contents. Does not call
	 * [onChange]. [factory] is accepted as `(CompoundTag) -> NBTHolder` rather than `(CompoundTag) ->
	 * T` so this stays callable through a star-projected receiver (needed for [NBTHolder]'s own
	 * bulk `loadFromTag`, which reloads every declared nested field generically without knowing each
	 * one's own [T]) - every real caller's [factory] only ever builds [T] instances regardless, so
	 * the cast back is safe in practice even though it isn't statically checked here.
	 */
	@Suppress("UNCHECKED_CAST")
	internal fun loadFrom(compound: NbtCompound?, factory: (CompoundTag) -> NBTHolder) {
		entries.clear()
		compound?.forEach { (key, tag) ->
			val subTag = (tag as? NbtCompound)?.toMinecraft ?: return@forEach
			entries[key] = (factory(subTag).also { it.loadFromTag(subTag) }) as T
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
