package net.kernelpanicsoft.archie.gui.layout

import net.kernelpanicsoft.archie.gui.modifiers.Constraints
import kotlin.math.max

/**
 * Base [MeasurePolicy] for [Row] and [Column] layouts.
 *
 * Handles sequential measurement (subtracting consumed space when [sumWidth] or [sumHeight]
 * is `true`) and delegates child placement to [placeChildren].
 *
 * @param sumWidth           When `true`, each child's width is subtracted from the remaining
 *   max-width before the next child is measured (Row behaviour).
 * @param sumHeight          When `true`, each child's height is subtracted from the remaining
 *   max-height before the next child is measured (Column behaviour).
 * @param arrangementSpacing Additional pixels added between siblings by the arrangement.
 */
abstract class RowColumnMeasurePolicy(
    val sumWidth: Boolean = false,
    val sumHeight: Boolean = false,
    val arrangementSpacing: Int = 0,
) : MeasurePolicy {

    override fun measure(scope: MeasureScope, measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        var remaining = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = ArrayList<Placeable>(measurables.size)
        var widthValue = 0
        var heightValue = 0

        for (index in measurables.indices) {
            val measured = measurables[index].measure(remaining)
            placeables += measured

            if (sumWidth) widthValue += measured.width else widthValue = max(widthValue, measured.width)
            if (sumHeight) heightValue += measured.height else heightValue = max(heightValue, measured.height)

            remaining = remaining.copy(
                maxWidth  = if (sumWidth)  (remaining.maxWidth  - measured.width).coerceAtLeast(0) else remaining.maxWidth,
                maxHeight = if (sumHeight) (remaining.maxHeight - measured.height).coerceAtLeast(0) else remaining.maxHeight,
            )
        }

        val extraSpacing = (arrangementSpacing * (placeables.size - 1)).coerceAtLeast(0)
        val width = if (sumWidth) widthValue + extraSpacing else widthValue
        val height = if (sumHeight) heightValue + extraSpacing else heightValue

        return placeChildren(
            scope, measurables, placeables,
            max(width,  constraints.minWidth),
            max(height, constraints.minHeight),
        )
    }

    /**
     * Positions all measured [placeables] within [width] × [height] and returns the
     * [MeasureResult].
     *
     * @param scope       The measuring [LayoutNode].
     * @param measurables The original measurables (for modifier access).
     * @param placeables  The measured placeables to position.
     * @param width       The resolved container width.
     * @param height      The resolved container height.
     */
    abstract fun placeChildren(
        scope: MeasureScope,
        measurables: List<Measurable>,
        placeables: List<Placeable>,
        width: Int,
        height: Int,
    ): MeasureResult
}
