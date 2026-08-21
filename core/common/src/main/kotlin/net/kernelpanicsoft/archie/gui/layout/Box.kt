package net.kernelpanicsoft.archie.gui.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.kernelpanicsoft.archie.gui.modifiers.Constraints
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.position.MarginModifier
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
		name = "Box",
		measurePolicy,
		modifier = modifier,
		content = content
	)
}

data class BoxMeasurePolicy(
    private val alignment: Alignment,
) : RowColumnMeasurePolicy() {
    
    override fun placeChildren(scope: MeasureScope, measurables: List<Measurable>, placeables: List<Placeable>, width: Int, height: Int): MeasureResult {
        return MeasureResult(width, height) {
            val inset = (scope as? LayoutNode)?.get<PaddingModifier>()?.padding
                ?: PaddingValues()
            var accumulatedOutset = 0
            for ((index, child) in placeables.withIndex()) {
                child.placeAt(alignment.align(child.size, IntSize(width, height), LayoutDirection.Ltr) + inset.getOffset())
                (measurables[index] as? LayoutNode)?.get<MarginModifier>()?.let { accumulatedOutset += it.horizontal + it.vertical }
            }
        }
    }
}