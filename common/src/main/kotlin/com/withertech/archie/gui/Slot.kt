package com.withertech.archie.gui

import androidx.compose.runtime.Composable
import com.withertech.archie.Archie
import com.withertech.archie.gui.layout.Layout
import com.withertech.archie.gui.layout.MeasureResult
import com.withertech.archie.gui.layout.Renderer
import com.withertech.archie.gui.modifiers.Modifier
import com.withertech.archie.gui.modifiers.sizeIn
import com.withertech.archie.gui.nodes.AUINode
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
				guiGraphics.blit(SLOT, x, y, 0, 0, 18, 18)
			}
		},
		modifier = Modifier.sizeIn(minWidth = 18, minHeight = 18).then(modifier)
	)
}