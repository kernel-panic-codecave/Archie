package com.withertech.archie.gui.layout

import androidx.compose.runtime.Stable
import com.withertech.archie.gui.nodes.AUINode
import com.withertech.archie.gui.modifiers.Constraints
import net.minecraft.client.gui.GuiGraphics

data class MeasureResult(
	val width: Int,
	val height: Int,
	val placer: Placer,
)

@Stable
fun interface MeasurePolicy {
	fun measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult
}

@Stable
fun interface Placer {
	fun placeChildren()
}

@Stable
interface Renderer {
	fun render(node: AUINode, x: Int, y: Int, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {}
	fun renderAfterChildren(node: AUINode, x: Int, y: Int, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {}
}


interface Measurable {
	fun measure(constraints: Constraints): Placeable
}

interface Placeable {
	var width: Int
	var height: Int

	fun placeAt(x: Int, y: Int)

	fun placeAt(offset: IntOffset) = placeAt(offset.x, offset.y)

	val size: IntSize get() = IntSize(width, height)
}