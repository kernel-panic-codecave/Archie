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
        val placeables = measurables.map { element ->
            val measured = element.measure(remaining)
            remaining = remaining.copy(
                maxWidth  = if (sumWidth)  (remaining.maxWidth  - measured.width).coerceAtLeast(0) else remaining.maxWidth,
                maxHeight = if (sumHeight) (remaining.maxHeight - measured.height).coerceAtLeast(0) else remaining.maxHeight,
            )
            measured
        }
        val extraSpacing = (arrangementSpacing * (placeables.size - 1)).coerceAtLeast(0)
        val width  = if (sumWidth)  placeables.sumOf { it.width }  + extraSpacing else placeables.maxOfOrNull { it.width }  ?: 0
        val height = if (sumHeight) placeables.sumOf { it.height } + extraSpacing else placeables.maxOfOrNull { it.height } ?: 0
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
