package net.kernelpanicsoft.archie.gui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.gui.layout.*
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.onGloballyPositioned
import net.kernelpanicsoft.archie.gui.modifiers.sizeIn
import net.kernelpanicsoft.archie.gui.nodes.AUINode
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation

data class SlotData(
	val groups: MutableMap<String, SlotGroup> = mutableMapOf()
)

data class SlotGroup(
	var x: Int = 0,
	var y: Int = 0,
	var width: Int = 0,
	var height: Int = 0,
	var slots: MutableSet<IntCoordinates> = mutableSetOf()
)

val LocalSlotData = compositionLocalOf { SlotData() }
val LocalSlotGroup = compositionLocalOf { SlotGroup() }

@Composable
fun Slots(
	id: String,
	width: Int,
	height: Int,
	content: @Composable () -> Unit
): SlotGroup
{
	val group = SlotGroup(
		0,
		0,
		width,
		height,
	)
	val data = LocalSlotData.current
	data.groups[id] = group

	Box(
		Modifier.onGloballyPositioned { coordinates ->
			val (x, y) = coordinates
			group.x = x
			group.y = y
			data.groups[id] = group
		}
	) {
		CompositionLocalProvider(LocalSlotGroup provides group) {
			content()
		}
	}
	return group
}

@Composable
fun Slot(modifier: Modifier = Modifier)
{
	val data = LocalSlotGroup.current
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
		modifier = Modifier.sizeIn(minWidth = 18, minHeight = 18).onGloballyPositioned { pos ->
			data.slots.add(pos)
			Archie.LOGGER.info("Slot $pos")
		}.then(modifier)
	)
}