package net.kernelpanicsoft.archie.serialization

import earth.terrarium.common_storage_lib.storage.base.UpdateManager
import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.NbtTag
import net.benwoodworth.knbt.buildNbtCompound

/**
 * A dynamically-sized, key-ordered map of storage-like [UpdateManager]s (e.g. [net.kernelpanicsoft.archie.transfer.ArchieItemStorage]),
 * declared via [NBTHolder.itemMapField]/`fluidMapField`/`energyMapField`. Unlike [NestedNBTHolderMap],
 * every entry shares the same shape (size/limit/capacity), fixed once at field declaration, so
 * [factory] is supplied once here rather than per [getOrPut] call.
 */
class ArchieStorageMap<T : UpdateManager<NbtTag>> internal constructor(
	private val factory: () -> T,
	private val onChange: () -> Unit,
) : Iterable<Map.Entry<String, T>> {
	private val entries: MutableMap<String, T> = mutableMapOf()

	val keys: Set<String> get() = entries.keys
	val size: Int get() = entries.size

	operator fun get(key: String): T? = entries[key]
	fun containsKey(key: String): Boolean = key in entries

	/** Returns the existing storage at [key], or builds one via [factory], stores it, and marks this dirty. */
	fun getOrPut(key: String): T {
		val existing = entries[key]
		if (existing != null) return existing
		val created = factory()
		entries[key] = created
		onChange()
		return created
	}

	/** Removes and returns the storage at [key], marking this dirty - `null` if it wasn't present. */
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

	override fun iterator(): Iterator<Map.Entry<String, T>> = entries.entries.iterator()

	/** Rebuilds every entry (each freshly built via [factory], then hydrated from its snapshot) from [compound]. */
	internal fun loadFrom(compound: NbtCompound?) {
		entries.clear()
		compound?.forEach { (key, snapshot) -> entries[key] = factory().apply { readSnapshot(snapshot) } }
	}

	/** Snapshots every entry, keyed by its map key. */
	internal fun toNbtCompound(): NbtCompound = buildNbtCompound {
		for ((key, storage) in entries) put(key, storage.createSnapshot())
	}
}

/**
 * A dynamically-sized, index-ordered list of storage-like [UpdateManager]s, declared via
 * [NBTHolder.itemListField]/`fluidListField`/`energyListField`. Every entry shares the same shape
 * (size/limit/capacity), built via [factory] on [add].
 *
 * Persisted as an [NbtCompound] keyed by stringified index, not a genuine NBT list - a raw NBT
 * `ListTag` requires every element be the *same* concrete tag type, which knbt only exposes a
 * public builder API for when that type is statically known. [T]'s snapshot shape is uniform at
 * runtime (every entry is the same concrete storage type) but erased to plain [NbtTag] at compile
 * time, so it can't use that API; a compound sidesteps the restriction entirely; ordering is
 * still preserved by parsing/reassigning indices on save.
 */
class ArchieStorageList<T : UpdateManager<NbtTag>> internal constructor(
	private val factory: () -> T,
	private val onChange: () -> Unit,
) : Iterable<T> {
	private val entries: MutableList<T> = mutableListOf()

	val size: Int get() = entries.size
	operator fun get(index: Int): T = entries[index]

	/** Builds a new entry via [factory], appends it, marks this dirty, and returns it. */
	fun add(): T {
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

	override fun iterator(): Iterator<T> = entries.iterator()

	/** Rebuilds every entry, in index order, from [compound]'s stringified-index keys. */
	internal fun loadFrom(compound: NbtCompound?) {
		entries.clear()
		compound ?: return
		compound.keys.mapNotNull { it.toIntOrNull() }.sorted().forEach { index ->
			entries += factory().apply { readSnapshot(compound.getValue(index.toString())) }
		}
	}

	/** Snapshots every entry under its (reassigned, 0-based) index as a string key. */
	internal fun toNbtCompound(): NbtCompound = buildNbtCompound {
		entries.forEachIndexed { index, storage -> put(index.toString(), storage.createSnapshot()) }
	}
}
