package net.kernelpanicsoft.archie.gui.nodes

import net.kernelpanicsoft.archie.gui.layout.LayoutNode
import net.kernelpanicsoft.archie.gui.layout.MeasurePolicy
import net.kernelpanicsoft.archie.gui.layout.Renderer
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
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
}