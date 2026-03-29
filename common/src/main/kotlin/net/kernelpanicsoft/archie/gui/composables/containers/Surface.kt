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
import net.kernelpanicsoft.archie.gui.theme.NinePatchThemeState
import net.kernelpanicsoft.archie.gui.theme.SimpleThemeState
import net.kernelpanicsoft.archie.gui.util.extension.ninePatchTexture
import net.minecraft.client.gui.GuiGraphics


@Composable
fun Surface(
	contentAlignment: Alignment = Alignment.TopStart,
	modifier: Modifier = Modifier,
	texture: String = "surface",
	content: @Composable () -> Unit
) {
	val measurePolicy = remember(contentAlignment) { BoxMeasurePolicy(contentAlignment) }
	val theme = LocalTheme.current
	val composableTheme = theme.getComposableTheme(texture)
	val state = composableTheme.getState(TextureStates.DEFAULT, theme.mode)

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
				if (composableTheme.isNinepatch) return guiGraphics.ninePatchTexture(
					x,
					y,
					node.width,
					node.height,
					state as NinePatchThemeState
				)

				guiGraphics.blit(
					(state as SimpleThemeState).texture,
					x,
					y,
					state.width,
					state.height,
					state.u.toFloat(),
					state.v.toFloat(),
					state.uWidth,
					state.vHeight,
					state.textureSize.width,
					state.textureSize.height,
				)

				super.render(node, x, y, guiGraphics, mouseX, mouseY, partialTick)
			}
		},
		modifier = Modifier.debug(state.texture.toString()).apply {
			if (!composableTheme.isNinepatch) with(composableTheme.states["default"]!!) {
				sizeIn(
					minWidth = textureSize.width,
					minHeight = textureSize.height
				)
			}
		} then modifier,
		content = content
	)
}