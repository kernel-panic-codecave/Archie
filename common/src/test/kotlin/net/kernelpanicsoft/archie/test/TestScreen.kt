package net.kernelpanicsoft.archie.test

import androidx.compose.runtime.Composable
import net.kernelpanicsoft.archie.gui.*
import net.kernelpanicsoft.archie.gui.layout.*
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class TestScreen(menu: TestMenu, playerInventory: Inventory, title: Component) : ComposeContainerScreen<TestMenu>(menu, playerInventory,
	title
)
{
	private val rows = menu.rows

	init
	{
		this.imageHeight = 114 + rows * 18
		this.inventoryLabelY = this.imageHeight - 94
		start {
			content()
		}
	}

	@Composable
	fun content()
	{
		Column {
			for (i in 0 until rows)
			{
				Row {
					for (j in 0 until 9)
					{
						Slot()
					}
				}
			}
		}
	}

//	override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int)
//	{
//		val i = (this.width - this.imageWidth) / 2
//		val j = (this.height - this.imageHeight) / 2
//		guiGraphics.blit(
//			CONTAINER_BACKGROUND, i, j, 0, 0,
//			this.imageWidth,
//			rows * 18 + 17
//		)
//		guiGraphics.blit(
//			CONTAINER_BACKGROUND, i, j + rows * 18 + 17, 0, 126,
//			this.imageWidth, 96
//		)
//	}

//	companion object
//	{
//		private val CONTAINER_BACKGROUND = ResourceLocation.parse("textures/gui/container/generic_54.png")
//	}
}