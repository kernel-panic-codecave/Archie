package net.kernelpanicsoft.archie.gui.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.position.MarginModifier
import net.kernelpanicsoft.archie.gui.modifiers.position.PaddingModifier
import net.kernelpanicsoft.archie.gui.modifiers.position.PaddingValues

/**
 * A layout composable that arranges its children in a vertical sequence from top to bottom.
 *
 * Children are measured sequentially and their heights subtracted from the available space.
 * Use [verticalArrangement] to control spacing and placement along the main axis, and
 * [horizontalAlignment] to align children along the cross axis.
 *
 * ### Example
 * ```kotlin
 * Column(
 *     verticalArrangement = Arrangement.spacedBy(8),
 *     horizontalAlignment = Alignment.CenterHorizontally,
 * ) {
 *     Text(Component.literal("Title"))
 *     Text(Component.literal("Subtitle"))
 * }
 * ```
 *
 * @param modifier            Modifiers applied to the Column node.
 * @param verticalArrangement Controls spacing and placement along the vertical axis.
 * @param horizontalAlignment Controls alignment of children along the horizontal axis.
 * @param content             The child composables to lay out in a column.
 */
@Composable
fun Column(
	modifier: Modifier = Modifier,
	verticalArrangement: Arrangement.Vertical = Arrangement.Top,
	horizontalAlignment: Alignment.Horizontal = Alignment.Start,
	content: @Composable () -> Unit
) {
	val measurePolicy = remember(verticalArrangement, horizontalAlignment) {
		ColumnMeasurePolicy(
			verticalArrangement,
			horizontalAlignment
		)
	}
	Layout(
		name = "Column",
		measurePolicy,
		modifier = modifier,
		content = content
	)
}

data class ColumnMeasurePolicy(
	private val verticalArrangement: Arrangement.Vertical,
	private val horizontalAlignment: Alignment.Horizontal,
) : RowColumnMeasurePolicy(
	sumHeight = true,
	arrangementSpacing = verticalArrangement.spacing
) {
	override fun placeChildren(scope: MeasureScope, measurables: List<Measurable>, placeables: List<Placeable>, width: Int, height: Int): MeasureResult {
		val childCount = placeables.size
		val positions = IntArray(childCount)
		val sizes = IntArray(childCount)
		for (index in 0 until childCount) {
			sizes[index] = placeables[index].height
		}

		verticalArrangement.arrange(
			totalSize = height,
			sizes = sizes,
			outPositions = positions
		)

		return MeasureResult(width, height) {
			val inset = (scope as? LayoutNode)?.get<PaddingModifier>()?.padding
				?: PaddingValues()
			var accumulatedOutset = 0

			for (index in 0 until childCount) {
				val child = placeables[index]
				child.placeAt(horizontalAlignment.align(child.width, width, LayoutDirection.Ltr) + inset.left, positions[index] + accumulatedOutset + inset.top)
				(measurables[index] as? LayoutNode)?.get<MarginModifier>()?.let { accumulatedOutset += it.vertical }
			}
		}
	}
}