package net.kernelpanicsoft.archie.serialization

import java.util.function.BiFunction
import java.util.function.Function

/**
 * A [MutableMap] wrapper that invokes [listener] with the underlying [map] after every
 * mutating operation. Used by [mapField]-style [NBTHolder] delegates to detect changes and
 * persist/sync them.
 */
class ObservableMap<K, V>(private val map: MutableMap<K, V>, private val listener: (MutableMap<K, V>) -> Unit) : MutableMap<K, V> by map
{
	override fun put(key: K, value: V): V?
	{
		return map.put(key, value).also { listener(map) }
	}

	override fun remove(key: K): V?
	{
		return map.remove(key).also { listener(map) }
	}

	override fun clear()
	{
		map.clear().also { listener(map) }
	}
	
	override fun putAll(from: Map<out K, V>)
	{
		map.putAll(from).also { listener(map) }
	}

	override fun remove(key: K, value: V): Boolean
	{
		return map.remove(key, value).also { listener(map) }
	}

	override fun replace(key: K, value: V): V?
	{
		return map.replace(key, value).also { listener(map) }
	}

	override fun replace(key: K, oldValue: V, newValue: V): Boolean
	{
		return map.replace(key, oldValue, newValue).also { listener(map) }
	}

	override fun replaceAll(function: BiFunction<in K, in V, out V>)
	{
		map.replaceAll(function).also { listener(map) }
	}

	override fun computeIfAbsent(key: K, mappingFunction: Function<in K, out V>): V
	{
		return map.computeIfAbsent(key, mappingFunction).also { listener(map) }
	}

	override fun putIfAbsent(key: K, value: V): V?
	{
		return map.putIfAbsent(key, value).also { listener(map) }
	}

	override fun computeIfPresent(
		key: K,
		remappingFunction: BiFunction<in K, in V & Any, out V?>
	): V?
	{
		return map.computeIfPresent(key, remappingFunction).also { listener(map) }
	}

	override fun compute(key: K, remappingFunction: BiFunction<in K, in V?, out V?>): V?
	{
		return map.compute(key, remappingFunction).also { listener(map) }
	}

	override fun merge(
		key: K,
		value: V & Any,
		remappingFunction: BiFunction<in V & Any, in V & Any, out V?>
	): V?
	{
		return map.merge(key, value, remappingFunction).also { listener(map) }
	}
}