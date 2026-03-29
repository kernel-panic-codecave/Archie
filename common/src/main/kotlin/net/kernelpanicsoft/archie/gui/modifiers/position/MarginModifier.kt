package net.kernelpanicsoft.archie.gui.modifiers.position

import androidx.compose.runtime.Stable
import net.kernelpanicsoft.archie.gui.layout.IntCoordinates
import net.kernelpanicsoft.archie.gui.modifiers.LayoutChangingModifier
import net.kernelpanicsoft.archie.gui.modifiers.Modifier

/**
 * Holds the four-sided margin values used by [MarginModifier].
 *
 * @property left   Left margin in pixels.
 * @property right  Right margin in pixels.
 * @property top    Top margin in pixels.
 * @property bottom Bottom margin in pixels.
 */
data class MarginValues(
    val left: Int = 0,
    val right: Int = 0,
    val top: Int = 0,
    val bottom: Int = 0,
) {
    /** Returns the top-left corner offset (left, top) as an [IntCoordinates]. */
    fun getOffset(): IntCoordinates = IntCoordinates(left, top)

    operator fun plus(other: MarginValues): MarginValues = MarginValues(
        left + other.left, right + other.right, top + other.top, bottom + other.bottom,
    )
}

/**
 * A [Modifier.Element] that adds outer spacing (margin) around a composable.
 *
 * Margins are applied **outside** the node bounds and are accumulated additively when
 * multiple [MarginModifier] elements are chained.
 *
 * @property margin The [MarginValues] describing each side's margin.
 */
data class MarginModifier(val margin: MarginValues) : Modifier.Element<MarginModifier>, LayoutChangingModifier {
    override fun mergeWith(other: MarginModifier): MarginModifier = MarginModifier(margin + other.margin)

    /** Total horizontal margin (left + right). */
    val horizontal get() = margin.left + margin.right

    /** Total vertical margin (top + bottom). */
    val vertical get() = margin.top + margin.bottom

    override fun modifyPosition(offset: IntCoordinates): IntCoordinates = offset + margin.getOffset()

    override fun toString(): String = buildString {
        append("MarginModifier(")
        val sides = buildList {
            if (margin.left   != 0) add("left=${margin.left}")
            if (margin.right  != 0) add("right=${margin.right}")
            if (margin.top    != 0) add("top=${margin.top}")
            if (margin.bottom != 0) add("bottom=${margin.bottom}")
        }
        append(sides.joinToString(", "))
        append(")")
    }
}

/**
 * Adds independent per-side margins around this composable.
 *
 * @param left   Left margin in pixels.
 * @param right  Right margin in pixels.
 * @param top    Top margin in pixels.
 * @param bottom Bottom margin in pixels.
 */
@Stable
fun Modifier.margin(left: Int = 0, right: Int = 0, top: Int = 0, bottom: Int = 0): Modifier =
    this then MarginModifier(MarginValues(left, right, top, bottom))

/**
 * Adds symmetric horizontal and vertical margins.
 *
 * @param horizontal Margin applied to both the left and right sides.
 * @param vertical   Margin applied to both the top and bottom sides.
 */
@Stable
fun Modifier.margin(horizontal: Int = 0, vertical: Int = 0): Modifier =
    margin(horizontal, horizontal, vertical, vertical)

/**
 * Adds a uniform margin on all four sides.
 *
 * @param all The margin in pixels applied to every side.
 */
@Stable
fun Modifier.margin(all: Int = 0): Modifier = margin(all, all, all, all)
