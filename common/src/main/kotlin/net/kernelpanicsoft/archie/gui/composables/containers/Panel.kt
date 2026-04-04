package net.kernelpanicsoft.archie.gui.composables.containers

import androidx.compose.runtime.Composable
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.Box
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.position.padding

/**
 * A padded themed surface used as a general-purpose container for grouped UI content.
 */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    texture: String = "surface",
    contentPadding: Int = 4,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        texture = texture,
        contentAlignment = contentAlignment,
    ) {
        Box(modifier = Modifier.padding(contentPadding), contentAlignment = contentAlignment) {
            content()
        }
    }
}

