package net.kernelpanicsoft.archie.gui.composables.containers

import androidx.compose.runtime.Composable
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.Box
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.position.padding
import net.kernelpanicsoft.archie.gui.modifiers.width
import net.kernelpanicsoft.archie.gui.theme.ThemeVariants

/**
 * A padded themed [Surface] used as a general-purpose container for grouped UI content.
 *
 * @param contentAlignment Alignment of [content] within the panel.
 * @param contentWidth When non-null, the panel's inner content area is fixed to this width
 *   (in pixels); the panel itself is sized to fit that plus [contentPadding] on both sides.
 * @param texture The themed texture/style key drawn as the panel's background. See [Surface].
 * @param variant The theme variant of [texture] to use. See [ThemeVariants].
 * @param contentPadding Padding (in pixels) inserted between the panel edge and [content].
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

