package net.kernelpanicsoft.archie.gui.composables.containers

import androidx.compose.runtime.Composable
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.Box
import net.kernelpanicsoft.archie.gui.layout.BoxMeasurePolicy
import net.kernelpanicsoft.archie.gui.layout.EmptyRenderer
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.fillMaxSize

/**
 * The top-level layout node for a screen's content, centered within the full screen bounds.
 *
 * [ComposeScreen][net.kernelpanicsoft.archie.gui.ComposeScreen] and
 * [ComposeContainerScreen][net.kernelpanicsoft.archie.gui.ComposeContainerScreen] wrap their
 * `start` content in this composable so [content] is measured/placed like a [Box] (children
 * stacked and top-start-aligned by default) while the whole subtree stays centered on screen.
 *
 * @param modifier Additional modifiers applied to the content layout node.
 */
@Composable
fun RootContainer(
	modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
	Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
		Layout(
			name = "RootContainer",
			measurePolicy = BoxMeasurePolicy(Alignment.TopStart),
			renderer = EmptyRenderer,
			modifier = modifier,
			content = content
		)
	}
}
