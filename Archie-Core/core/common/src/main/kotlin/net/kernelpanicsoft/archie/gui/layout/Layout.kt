package net.kernelpanicsoft.archie.gui.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.nodes.UINode
import net.kernelpanicsoft.archie.gui.nodes.LayoutNodeApplier

/**
 * The fundamental building block for creating custom Compose-based UI elements in Archie.
 *
 * [Layout] is the lowest-level composable: it emits a single [UINode] into the composition
 * tree and wires up measurement, rendering, and modifier behaviour via the provided policies.
 * Higher-level composables such as [Box], [Row], [Column], and all built-in widgets are
 * implemented in terms of [Layout].
 *
 * ### Creating a custom composable
 * ```kotlin
 * @Composable
 * fun MyBox(modifier: Modifier = Modifier) {
 *     Layout(
 *         measurePolicy = { measurables, constraints ->
 *             val placeables = measurables.map { it.measure(constraints) }
 *             MeasureResult(constraints.maxWidth, constraints.maxHeight) {
 *                 placeables.forEach { it.placeAt(0, 0) }
 *             }
 *         },
 *         renderer = object : Renderer {
 *             override fun render(node, x, y, guiGraphics, mouseX, mouseY, partialTick) {
 *                 guiGraphics.fill(x, y, x + node.width, y + node.height, 0xFFFF0000.toInt())
 *             }
 *         },
 *         modifier = modifier,
 *     )
 * }
 * ```
 *
 * @param measurePolicy Defines how this node and its children are measured and placed.
 * @param renderer      Defines how this node renders itself. Defaults to [EmptyRenderer].
 * @param modifier      [Modifier] chain applied to this node.
 * @param content       Child composables emitted inside this node.
 */
@Composable
inline fun Layout(
	name: String,
	measurePolicy: MeasurePolicy,
	renderer: Renderer = EmptyRenderer,
	modifier: Modifier = Modifier,
	content: @Composable () -> Unit = {}
) {
	ComposeNode<LayoutNode, LayoutNodeApplier>(
		factory = { LayoutNode(name) },
		update = {
			set(measurePolicy) { this.measurePolicy = it }
			set(renderer) { this.renderer = it }
			set(modifier) { this.modifier = it }
		},
		content = content,
	)
}