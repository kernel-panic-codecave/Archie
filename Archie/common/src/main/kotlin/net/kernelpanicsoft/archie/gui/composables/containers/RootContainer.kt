package net.kernelpanicsoft.archie.gui.composables.containers

import androidx.compose.runtime.Composable
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.Box
import net.kernelpanicsoft.archie.gui.layout.BoxMeasurePolicy
import net.kernelpanicsoft.archie.gui.layout.EmptyRenderer
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.fillMaxSize

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
