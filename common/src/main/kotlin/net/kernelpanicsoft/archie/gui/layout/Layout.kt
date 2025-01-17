package net.kernelpanicsoft.archie.gui.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.nodes.AUINode
import net.kernelpanicsoft.archie.gui.nodes.AUINodeApplier

/**
 * The main component for layout, it measures and positions zero or more children.
 */
@Composable
inline fun Layout(
	measurePolicy: MeasurePolicy,
	renderer: Renderer = EmptyRenderer,
	modifier: Modifier = Modifier,
	content: @Composable () -> Unit = {}
) {
	ComposeNode<AUINode, AUINodeApplier>(
		factory = AUINode.Constructor,
		update = {
			set(measurePolicy) { this.measurePolicy = it }
			set(renderer) { this.renderer = it }
			set(modifier) { this.modifier = it }
		},
		content = content,
	)
}