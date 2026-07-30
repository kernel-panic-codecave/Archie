package net.kernelpanicsoft.archie.util

/** A [Map.Entry] whose [key] and [value] can be reassigned, unlike the standard read-only entry. */
data class MutableEntry<K, V>(
	override var key: K,
	override var value: V
) : Map.Entry<K, V>

/** Converts a [Pair] into a [MutableEntry]. */
fun <K, V> Pair<K, V>.toMutableEntry(): MutableEntry<K, V> = MutableEntry(first, second)
/** Copies a [Map.Entry] into a standalone, mutable [MutableEntry]. */
fun <K, V> Map.Entry<K, V>.toMutableEntry(): MutableEntry<K, V> = MutableEntry(key, value)
