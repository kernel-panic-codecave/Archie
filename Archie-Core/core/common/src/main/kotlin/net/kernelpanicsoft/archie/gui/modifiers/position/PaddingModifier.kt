package net.kernelpanicsoft.archie.gui.modifiers.position

import androidx.compose.runtime.Stable
import net.kernelpanicsoft.archie.gui.layout.IntCoordinates
import net.kernelpanicsoft.archie.gui.modifiers.Constraints
import net.kernelpanicsoft.archie.gui.modifiers.LayoutChangingModifier
import net.kernelpanicsoft.archie.gui.modifiers.Modifier

/**
 * Holds the four-sided padding values used by [PaddingModifier].
 *
 * @property left   Left padding in pixels.
 * @property right  Right padding in pixels.
 * @property top    Top padding in pixels.
 * @property bottom Bottom padding in pixels.
 */
data class PaddingValues(
    val left: Int = 0,
    val right: Int = 0,
    val top: Int = 0,
    val bottom: Int = 0,
) {
    /** Returns the top-left corner offset (left, top) as an [IntCoordinates]. */
    fun getOffset(): IntCoordinates = IntCoordinates(left, top)

    operator fun plus(other: PaddingValues): PaddingValues = PaddingValues(
        left + other.left, right + other.right, top + other.top, bottom + other.bottom,
    )
}

/**
 * A [Modifier.Element] that adds inner spacing (padding) inside a composable.
 *
 * Padding is applied **inside** the node bounds and reduces the available space for children.
 *
 * @property padding The [PaddingValues] describing each side's padding.
 */
data class PaddingModifier(val padding: PaddingValues) : Modifier.Element<PaddingModifier>, LayoutChangingModifier {
    override fun mergeWith(other: PaddingModifier): PaddingModifier = PaddingModifier(padding + other.padding)

    /** Total horizontal padding (left + right). */
    val horizontal get() = padding.left + padding.right

    /** Total vertical padding (top + bottom). */
    val vertical get() = padding.top + padding.bottom

    override fun modifyInnerConstraints(constraints: Constraints): Constraints =
        constraints.copy(
            maxWidth = (constraints.maxWidth - horizontal).coerceAtLeast(0),
            maxHeight = (constraints.maxHeight - vertical).coerceAtLeast(0),
            minWidth = (constraints.minWidth - horizontal).coerceAtLeast(0),
            minHeight = (constraints.minHeight - vertical).coerceAtLeast(0),
        )

    override fun toString(): String = buildString {
        append("PaddingModifier(")
        val sides = buildList {
            if (padding.left   != 0) add("left=${padding.left}")
            if (padding.right  != 0) add("right=${padding.right}")
            if (padding.top    != 0) add("top=${padding.top}")
            if (padding.bottom != 0) add("bottom=${padding.bottom}")
        }
        append(sides.joinToString(", "))
        append(")")
    }
}

/**
 * Adds independent per-side padding inside this composable.
 *
 * @param left   Left padding in pixels.
 * @param right  Right padding in pixels.
 * @param top    Top padding in pixels.
 * @param bottom Bottom padding in pixels.
 */
@Stable
fun Modifier.padding(left: Int = 0, right: Int = 0, top: Int = 0, bottom: Int = 0): Modifier =
    this then PaddingModifier(PaddingValues(left, right, top, bottom))

/**
 * Adds symmetric horizontal and vertical padding.
 *
 * @param horizontal Padding applied to both the left and right sides.
 * @param vertical   Padding applied to both the top and bottom sides.
 */
@Stable
fun Modifier.padding(horizontal: Int = 0, vertical: Int = 0): Modifier =
    padding(horizontal, horizontal, vertical, vertical)

/**
 * Adds uniform padding on all four sides.
 *
 * @param all The padding in pixels applied to every side.
 */
@Stable
fun Modifier.padding(all: Int = 0): Modifier = padding(all, all, all, all)
