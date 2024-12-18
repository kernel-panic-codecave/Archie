package com.withertech.archie.gui.nodes

import com.withertech.archie.gui.layout.LayoutNode
import com.withertech.archie.gui.layout.MeasurePolicy
import com.withertech.archie.gui.layout.Renderer
import com.withertech.archie.gui.modifiers.Modifier
import net.minecraft.client.gui.GuiGraphics

interface AUINode {
	var measurePolicy: MeasurePolicy
	var renderer: Renderer
	var modifier: Modifier
	var width: Int
	var height: Int
	var x: Int
	var y: Int

	fun render(x: Int, y: Int, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float)

	companion object {
		val Constructor: () -> AUINode = ::LayoutNode
	}
}