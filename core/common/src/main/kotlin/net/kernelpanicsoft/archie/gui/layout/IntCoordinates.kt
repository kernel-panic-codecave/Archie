package net.kernelpanicsoft.archie.gui.layout

import kotlinx.serialization.Serializable

/**
 * A 2D integer coordinate pair, packed into a single [Long] (`x` in the high 32 bits, `y` in the
 * low 32 bits) to avoid boxing allocations. Also aliased as [IntOffset] when used to represent a
 * relative displacement rather than an absolute position.
 */
@JvmInline
@Serializable
value class IntCoordinates(val pair: Long) {
	val x get() = (pair shr 32).toInt()
	val y get() = pair.toInt()

	operator fun component1() = x
	operator fun component2() = y

	// y.toLong() alone sign-extends a negative y across the upper 32 bits - the ones this OR
	// packs x into - clobbering x to -1 regardless of its real value. Masking to the low 32
	// bits keeps y's packed bit pattern (still decoded correctly by pair.toInt(), which only
	// ever reads those same low bits) without corrupting x's half.
	constructor(x: Int, y: Int) : this((x.toLong() shl 32) or (y.toLong() and 0xFFFFFFFFL))

	override fun toString(): String = "($x, $y)"

	operator fun plus(other: IntCoordinates) = IntCoordinates(x + other.x, y + other.y)
	operator fun minus(other: IntCoordinates) = IntCoordinates(x - other.x, y - other.y)
}

/** An [IntCoordinates] used to represent a relative displacement rather than an absolute position. */
typealias IntOffset = IntCoordinates

/**
 * An integer width/height pair, packed into a single [Long] (`width` in the high 32 bits,
 * `height` in the low 32 bits) to avoid boxing allocations.
 */
@JvmInline
@Serializable
value class IntSize(val pair: Long) {
	val width get() = (pair shr 32).toInt()
	val height get() = pair.toInt()

	operator fun component1() = width
	operator fun component2() = height

	// See IntCoordinates' identically-shaped constructor for why height must be masked here too.
	constructor(width: Int, height: Int) : this((width.toLong() shl 32) or (height.toLong() and 0xFFFFFFFFL))

	override fun toString(): String = "($width, $height)"
}

/** Creates an [IntCoordinates] at the given [x], [y] position. */
fun pos(x: Int, y: Int) = IntCoordinates(x, y)

/** Creates an [IntOffset] with the given [x], [y] displacement, defaulting to zero. */
fun offset(x: Int = 0, y: Int = 0) = IntOffset(x, y)

/** Creates an [IntSize] with the given [width] and [height]. */
fun size(width: Int, height: Int) = IntSize(width, height)