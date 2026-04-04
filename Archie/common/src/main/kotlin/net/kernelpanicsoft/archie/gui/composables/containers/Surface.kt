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
import net.kernelpanicsoft.archie.gui.nodes.AUINode
import net.kernelpanicsoft.archie.gui.theme.LocalTheme
import net.kernelpanicsoft.archie.gui.theme.SimpleThemeState
import net.kernelpanicsoft.archie.gui.theme.ThemeVariants
import net.kernelpanicsoft.archie.gui.util.extension.drawThemeState
import net.minecraft.client.gui.GuiGraphics


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
				node: AUINode,
				x: Int,
				y: Int,
				guiGraphics: GuiGraphics,
				mouseX: Int,
				mouseY: Int,
				partialTick: Float
			) {
				guiGraphics.drawThemeState(state, x, y, node.width, node.height)

				super.render(node, x, y, guiGraphics, mouseX, mouseY, partialTick)
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