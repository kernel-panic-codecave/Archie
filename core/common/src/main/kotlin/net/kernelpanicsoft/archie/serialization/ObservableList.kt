package net.kernelpanicsoft.archie.serialization

import java.util.function.IntFunction
import java.util.function.Predicate
import java.util.function.UnaryOperator

/**
 * A [MutableList] wrapper that invokes [listener] with the underlying [list] after every
 * mutating operation. Used by [listField]-style [NBTHolder] delegates to detect changes and
 * persist/sync them.
 */
class ObservableList<T>(private val list: MutableList<T>, private val listener: (MutableList<T>) -> Unit) : MutableList<T> by list
{
	override fun add(element: T): Boolean
	{
		return list.add(element).also { listener(list) }
	}

	override fun add(index: Int, element: T)
	{
		return list.add(index, element).also { listener(list) }
	}

	override fun remove(element: T): Boolean
	{
		return list.remove(element).also { listener(list) }
	}

	override fun removeAt(index: Int): T
	{
		return list.removeAt(index).also { listener(list) }
	}

	override fun addAll(elements: Collection<T>): Boolean
	{
		return list.addAll(elements).also { listener(list) }
	}

	override fun removeAll(elements: Collection<T>): Boolean
	{
		return list.removeAll(elements).also { listener(list) }
	}

	override fun set(index: Int, element: T): T
	{
		return list.set(index, element).also { listener(list) }
	}

	override fun clear()
	{
		list.clear().also { listener(list) }
	}

	override fun addAll(index: Int, elements: Collection<T>): Boolean
	{
		return list.addAll(index, elements).also { listener(list) }
	}

	override fun removeIf(filter: Predicate<in T>): Boolean
	{
		return list.removeIf(filter).also { listener(list) }
	}

	override fun retainAll(elements: Collection<T>): Boolean
	{
		return list.retainAll(elements).also { listener(list) }
	}

	override fun replaceAll(operator: UnaryOperator<T>)
	{
		list.replaceAll(operator).also { listener(list) }
	}
	
	override fun sort(c: Comparator<in T>?)
	{
		list.sortWith(c!!).also { listener(list) }
	}

	override fun addFirst(e: T)
	{
		list.addFirst(e).also { listener(list) }
	}

	override fun addLast(e: T)
	{
		list.addLast(e).also { listener(list) }
	}

	override fun removeFirst(): T
	{
		return list.removeFirst().also { listener(list) }
	}
	
	override fun removeLast(): T
	{
		return list.removeLast().also { listener(list) }
	}

	/**
	 * Replaces the whole contents with [elements], notifying [listener] exactly **once**.
	 *
	 * The one bulk write. Every other mutator here notifies per operation, which for a persisted
	 * field means a whole-list encode per element touched - so a caller rewriting `n` entries one
	 * at a time pays `n` encodes of `n` entries, and the cost of holding a list grows with the
	 * square of its length. A caller that already knows the entire next state says so through this
	 * instead: copy the list out, work on the copy, hand it back in one call.
	 */
	fun setAll(elements: Collection<T>)
	{
		list.clear()
		list.addAll(elements)
		listener(list)
	}
}