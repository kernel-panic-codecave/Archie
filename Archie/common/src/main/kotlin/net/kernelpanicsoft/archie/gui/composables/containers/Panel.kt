package net.kernelpanicsoft.archie.gui.composables.containers

import androidx.compose.runtime.Composable
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.Box
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.position.padding
import net.kernelpanicsoft.archie.gui.modifiers.width
import net.kernelpanicsoft.archie.gui.theme.ThemeVariants

/**
 * A padded themed surface used as a general-purpose container for grouped UI content.
 */
@Composable
fun Panel(
	modifier: Modifier = Modifier,
	contentAlignment: Alignment = Alignment.TopStart,
	contentWidth: Int? = null,
	texture: String = "surface",
	variant: String = ThemeVariants.DEFAULT,
	contentPadding: Int = 8,
	content: @Composable () -> Unit,
) {
    val resolvedModifier = if (contentWidth != null) modifier.width(contentWidth + contentPadding*2) else modifier
    Surface(
        modifier = resolvedModifier,
        texture = texture,
        variant = variant,
        contentAlignment = contentAlignment,
    ) {
        Box(modifier = Modifier.padding(contentPadding), contentAlignment = contentAlignment) {
            content()
        }
    }
}

