package net.kernelpanicsoft.archie.gui

import androidx.compose.runtime.Composable
import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.layout.MeasureResult
import net.kernelpanicsoft.archie.gui.layout.Renderer
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.sizeIn
import net.kernelpanicsoft.archie.gui.nodes.AUINode
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation

@Composable
fun Slot(modifier: Modifier = Modifier) {
	Layout(
		measurePolicy = { _, constraints ->
			MeasureResult(constraints.minWidth, constraints.minHeight) {}
		},
		renderer = object : Renderer
		{
			private val SLOT = ResourceLocation.fromNamespaceAndPath(Archie.MOD_ID, "textures/gui/slot.png")
			override fun render(
				node: AUINode,
				x: Int,
				y: Int,
				guiGraphics: GuiGraphics,
				mouseX: Int,
				mouseY: Int,
				partialTick: Float
			)
			{
				guiGraphics.blit(SLOT, x, y, 18, 18, 0f, 0f, 18, 18, 18, 18)
			}
		},
		modifier = Modifier.sizeIn(minWidth = 18, minHeight = 18).then(modifier)
	)
}