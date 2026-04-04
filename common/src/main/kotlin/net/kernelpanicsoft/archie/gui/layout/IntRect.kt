package net.kernelpanicsoft.archie.gui.layout

import kotlinx.serialization.Serializable

@Serializable
data class IntRect(
    val minX: Int,
    val minY: Int,
    val maxX: Int,
    val maxY: Int,
) {
    val width: Int get() = maxX - minX
    val height: Int get() = maxY - minY

    fun isEmpty(): Boolean = width <= 0 || height <= 0

    fun intersect(other: IntRect): IntRect? {
        val ix = maxOf(minX, other.minX)
        val iy = maxOf(minY, other.minY)
        val ax = minOf(maxX, other.maxX)
        val ay = minOf(maxY, other.maxY)
        return if (ax <= ix || ay <= iy) null else IntRect(ix, iy, ax, ay)
    }

    companion object {
        val EMPTY: IntRect = IntRect(0, 0, 0, 0)

        fun fromPositionAndSize(position: IntCoordinates, size: Size): IntRect =
            IntRect(position.x, position.y, position.x + size.width, position.y + size.height)
    }
}

