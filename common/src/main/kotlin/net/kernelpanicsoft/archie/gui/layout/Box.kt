package net.kernelpanicsoft.archie.gui.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.kernelpanicsoft.archie.gui.modifiers.Constraints
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.position.PaddingModifier
import net.kernelpanicsoft.archie.gui.modifiers.position.PaddingValues

/**
 * A layout composable that stacks its children on top of each other, aligned within its bounds.
 *
 * Each child is independently aligned using [contentAlignment]. Children are drawn in
 * declaration order (first child at the bottom, last child on top).
 *
 * ### Example
 * ```kotlin
 * Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100, 60)) {
 *     // background fills the box
 *     Spacer(modifier = Modifier.fillMaxSize().background(KColor.DARK_GRAY))
 *     Text(Component.literal("Centered"))
 * }
 * ```
 *
 * @param modifier         Modifiers applied to the outer Box node.
 * @param contentAlignment How children are positioned within the box. Default [Alignment.TopStart].
 * @param content          The child composables to stack.
 */
@Composable
fun Box(
	modifier: Modifier = Modifier,
	contentAlignment: Alignment = Alignment.TopStart,
	content: @Composable () -> Unit
) {
	val measurePolicy = remember(contentAlignment) { BoxMeasurePolicy(contentAlignment) }
	Layout(
		measurePolicy,
		modifier = modifier,
		content = content
	)
}

internal data class BoxMeasurePolicy(
    private val alignment: Alignment,
) : RowColumnMeasurePolicy() {
    override fun measure(scope: MeasureScope, measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        // Measure children with intrinsic size constraints (minWidth=0, maxWidth=infinity)
        // This prevents size modifiers from forcing children to fill the Box.
        // The Box itself will be constrained by its own modifiers, but children are not.
        val intrinsicConstraints = Constraints(0, constraints.maxWidth, 0, constraints.maxHeight)
        val placeables = measurables.map { it.measure(intrinsicConstraints) }
        
        // Calculate Box dimensions based on content
        val width  = (placeables.maxOfOrNull { it.width }  ?: 0).coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = (placeables.maxOfOrNull { it.height } ?: 0).coerceIn(constraints.minHeight, constraints.maxHeight)
        
        return placeChildren(scope, measurables, placeables, width, height)
    }
    
    override fun placeChildren(scope: MeasureScope, measurables: List<Measurable>, placeables: List<Placeable>, width: Int, height: Int): MeasureResult {
        return MeasureResult(width, height) {
            val inset = (scope as? LayoutNode)?.get<PaddingModifier>()?.padding
                ?: PaddingValues()
            for (child in placeables) {
                child.placeAt(alignment.align(child.size, IntSize(width, height), LayoutDirection.Ltr) + inset.getOffset())
            }
        }
    }
}