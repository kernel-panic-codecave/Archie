package net.kernelpanicsoft.archie.gui.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.position.MarginModifier
import net.kernelpanicsoft.archie.gui.modifiers.position.PaddingModifier
import net.kernelpanicsoft.archie.gui.modifiers.position.PaddingValues

/**
 * A layout composable that arranges its children in a horizontal sequence from left to right.
 *
 * Children are measured sequentially and their widths subtracted from the available space.
 * Use [horizontalArrangement] to control spacing and alignment along the main axis, and
 * [verticalAlignment] to align children along the cross axis.
 *
 * ### Example
 * ```kotlin
 * Row(
 *     horizontalArrangement = Arrangement.spacedBy(8),
 *     verticalAlignment = Alignment.CenterVertically,
 * ) {
 *     Icon(...)
 *     Text(Component.literal("Label"))
 * }
 * ```
 *
 * @param modifier              Modifiers applied to the Row node.
 * @param horizontalArrangement Controls spacing and placement along the horizontal axis.
 * @param verticalAlignment     Controls alignment of children along the vertical axis.
 * @param content               The child composables to lay out in a row.
 */
@Composable
fun Row(
	modifier: Modifier = Modifier,
	horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
	verticalAlignment: Alignment.Vertical = Alignment.Top,
	content: @Composable () -> Unit
) {
	val measurePolicy = remember(horizontalArrangement, verticalAlignment) {
		RowMeasurePolicy(
			horizontalArrangement,
			verticalAlignment
		)
	}
	Layout(
		name = "Row",
		measurePolicy,
		modifier = modifier,
		content = content
	)
}

private data class RowMeasurePolicy(
    private val horizontalArrangement: Arrangement.Horizontal,
    private val verticalAlignment: Alignment.Vertical,
) : RowColumnMeasurePolicy(sumWidth = true, arrangementSpacing = horizontalArrangement.spacing) {
    override fun placeChildren(scope: MeasureScope, measurables: List<Measurable>, placeables: List<Placeable>, width: Int, height: Int): MeasureResult {
		val childCount = placeables.size
		val positions = IntArray(childCount)
		val sizes = IntArray(childCount)
		for (index in 0 until childCount) {
			sizes[index] = placeables[index].width
		}

		horizontalArrangement.arrange(totalSize = width, sizes = sizes, layoutDirection = LayoutDirection.Ltr, outPositions = positions)

        return MeasureResult(width, height) {
            val inset = (scope as? LayoutNode)?.get<PaddingModifier>()?.padding
                ?: PaddingValues()
            var accumulatedOutset = 0

			for (index in 0 until childCount) {
				val child = placeables[index]
				child.placeAt(positions[index] + accumulatedOutset + inset.left, verticalAlignment.align(child.height, height) + inset.top)
				(measurables[index] as? LayoutNode)?.get<MarginModifier>()?.let { accumulatedOutset += it.horizontal }
            }
        }
    }
}