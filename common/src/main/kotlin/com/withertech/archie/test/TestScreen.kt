package com.withertech.archie.test

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

class TestScreen(menu: TestMenu, playerInventory: Inventory, title: Component) : AbstractContainerScreen<TestMenu>(menu, playerInventory,
	title
)
{
	private val rows = menu.rows
	init
	{
		this.imageHeight = 114 + rows * 18
		this.inventoryLabelY = this.imageHeight - 94
	}

	override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float)
	{
		super.render(guiGraphics, mouseX, mouseY, partialTick)
		renderTooltip(guiGraphics, mouseX, mouseY)
	}

	override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int)
	{
		val i = (this.width - this.imageWidth) / 2
		val j = (this.height - this.imageHeight) / 2
		guiGraphics.blit(
			CONTAINER_BACKGROUND, i, j, 0, 0,
			this.imageWidth,
			rows * 18 + 17
		)
		guiGraphics.blit(
			CONTAINER_BACKGROUND, i, j + rows * 18 + 17, 0, 126,
			this.imageWidth, 96
		)
	}

	companion object
	{
		private val CONTAINER_BACKGROUND = ResourceLocation("textures/gui/container/generic_54.png")
	}
}