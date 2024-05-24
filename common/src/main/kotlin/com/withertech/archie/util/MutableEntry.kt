package com.withertech.archie.util

data class MutableEntry<K, V>(
	override var key: K,
	override var value: V
) : Map.Entry<K, V>

fun <K, V> Pair<K, V>.toMutableEntry(): MutableEntry<K, V> = MutableEntry(first, second)
fun <K, V> Map.Entry<K, V>.toMutableEntry(): MutableEntry<K, V> = MutableEntry(key, value)
