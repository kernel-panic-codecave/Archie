package net.kernelpanicsoft.archie.gui.composables.containers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.kernelpanicsoft.archie.gui.composables.theme.TextureStates
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.BoxMeasurePolicy
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.layout.Renderer
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.debug
import net.kernelpanicsoft.archie.gui.modifiers.sizeIn
import net.kernelpanicsoft.archie.gui.nodes.UINode
import net.kernelpanicsoft.archie.gui.theme.LocalTheme
import net.kernelpanicsoft.archie.gui.theme.SimpleThemeState
import net.kernelpanicsoft.archie.gui.theme.ThemeVariants
import net.kernelpanicsoft.archie.gui.util.extension.drawThemeState
import net.kernelpanicsoft.archie.gui.util.extension.invoke
import net.minecraft.client.gui.GuiGraphics


/**
 * A [net.kernelpanicsoft.archie.gui.layout.Box]-like layout node that paints a themed background texture behind its children.
 *
 * The texture is resolved from the current [LocalTheme] by [texture] key and [variant], and
 * is drawn nine-sliced if the theme defines it as such, otherwise stretched to fit like a
 * simple sprite (in which case the surface has a minimum size matching the sprite's own).
 * [Panel] builds on top of this to add content padding.
 *
 * @param contentAlignment Alignment of [content] within the surface, as in [net.kernelpanicsoft.archie.gui.layout.Box].
 * @param modifier         Additional modifiers applied to the layout node.
 * @param texture          The themed texture key to look up via [LocalTheme].
 * @param variant          The theme variant of [texture] to use. See [ThemeVariants].
 */
@Composable
fun Surface(
	contentAlignment: Alignment = Alignment.TopStart,
	modifier: Modifier = Modifier,
	texture: String = "surface",
	variant: String = ThemeVariants.DEFAULT,
	content: @Composable () -> Unit
) {
	val measurePolicy = remember(contentAlignment) { BoxMeasurePolicy(contentAlignment) }
	val theme = LocalTheme.current
	val composableTheme = theme.getComposableTheme(texture)
	val state = composableTheme.getState(TextureStates.DEFAULT, variant)

	Layout(
		name = "Surface",
		measurePolicy = measurePolicy,
		renderer = object : Renderer
		{
			override fun render(
				node: UINode,
				x: Int,
				y: Int,
				guiGraphics: GuiGraphics,
				mouseX: Int,
				mouseY: Int,
				partialTick: Float
			) = guiGraphics {
				drawThemeState(state, x, y, node.width, node.height)
			}
		},
		modifier = Modifier.debug(state.texture.toString()).apply {
			if (!composableTheme.isNineslice) {
				with(composableTheme.states[TextureStates.DEFAULT] as SimpleThemeState) {
					sizeIn(
						minWidth = width,
						minHeight = height
					)
				}
			}
		} then modifier,
		content = content
	)
}