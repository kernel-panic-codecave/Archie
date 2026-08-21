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
import net.kernelpanicsoft.archie.gui.nodes.UINode
import net.kernelpanicsoft.archie.gui.theme.LocalTheme
import net.kernelpanicsoft.archie.gui.theme.ThemeVariants
import net.kernelpanicsoft.archie.gui.theme.intrinsicSizeModifier
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
 * @param stateName        The [TextureStates] key to look up within [variant] - most callers
 *   never need to touch this ([TextureStates.DEFAULT], a plain idle surface, covers the common
 *   case), but a caller with its own non-interactive state axis (e.g. [NodeFrame]'s own
 *   obtained/unobtained) can select a different sprite per state the same way a themed widget
 *   like a button already does for its own interactive states.
 * @param drawOverContent  When `true`, [texture] draws after [content] instead of before, acting
 *   as an overlaid frame rather than a background - meant for a transparent-centered variant.
 */
@Composable
fun Surface(
	contentAlignment: Alignment = Alignment.TopStart,
	modifier: Modifier = Modifier,
	texture: String = "surface",
	variant: String = ThemeVariants.DEFAULT,
	stateName: String = TextureStates.DEFAULT,
	drawOverContent: Boolean = false,
	content: @Composable () -> Unit
) {
	val measurePolicy = remember(contentAlignment) { BoxMeasurePolicy(contentAlignment) }
	val theme = LocalTheme.current
	val composableTheme = theme.getComposableTheme(texture)
	val state = composableTheme.getState(stateName, variant)

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
			) {
				if (!drawOverContent) guiGraphics { drawThemeState(state, x, y, node.width, node.height) }
			}

			override fun renderAfterChildren(
				node: UINode,
				x: Int,
				y: Int,
				guiGraphics: GuiGraphics,
				mouseX: Int,
				mouseY: Int,
				partialTick: Float
			) {
				if (drawOverContent) guiGraphics { drawThemeState(state, x, y, node.width, node.height) }
			}
		},
		modifier = Modifier.debug(state.texture.toString()).then(composableTheme.intrinsicSizeModifier()) then modifier,
		content = content
	)
}