package net.kernelpanicsoft.archie.gui.layout

import kotlinx.serialization.Serializable

/**
 * An axis-aligned integer rectangle described by its min/max bounds along each axis, rather than
 * an origin and a size. Used for hit-testing and clip/overlap calculations (e.g. scissor regions).
 */
@Serializable
data class IntRect(
    val minX: Int,
    val minY: Int,
    val maxX: Int,
    val maxY: Int,
) {
    val width: Int get() = maxX - minX
    val height: Int get() = maxY - minY

    /** Returns `true` if this rect has zero or negative width/height. */
    fun isEmpty(): Boolean = width <= 0 || height <= 0

    /**
     * Returns the overlapping region between this rect and [other], or `null` if they don't
     * overlap.
     */
    fun intersect(other: IntRect): IntRect? {
        val ix = maxOf(minX, other.minX)
        val iy = maxOf(minY, other.minY)
        val ax = minOf(maxX, other.maxX)
        val ay = minOf(maxY, other.maxY)
        return if (ax <= ix || ay <= iy) null else IntRect(ix, iy, ax, ay)
    }

    companion object {
        /** A rect with zero bounds on every side. */
        val EMPTY: IntRect = IntRect(0, 0, 0, 0)

        /** Builds an [IntRect] from a top-left [position] and a [size]. */
        fun fromPositionAndSize(position: IntCoordinates, size: Size): IntRect =
            IntRect(position.x, position.y, position.x + size.width, position.y + size.height)
    }
}
