package net.kernelpanicsoft.archie.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import net.kernelpanicsoft.archie.gui.*
import net.kernelpanicsoft.archie.gui.composables.basic.Spacer
import net.kernelpanicsoft.archie.gui.composables.containers.ContainerScreenLayout
import net.kernelpanicsoft.archie.gui.layout.*
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.height
import net.kernelpanicsoft.archie.gui.modifiers.size
import net.kernelpanicsoft.archie.gui.modifiers.sizeIn
import net.kernelpanicsoft.archie.gui.theme.Theme
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class TestScreen(menu: TestMenu, playerInventory: Inventory, title: Component) : ComposeContainerScreen<TestMenu, TestTile>(menu, playerInventory,
	title
)
{
	private val rows = menu.rows

	init
	{
//		ArchieTest.LOGGER.info("Screen: $title")
//		this.imageHeight = 114 + rows * 18
//		this.inventoryLabelY = this.imageHeight - 94
		start {
			content()
		}
	}

	@Composable
	fun content()
	{
		Theme {
			ContainerScreenLayout {
				Slots(
					"inventory",
					9,
					rows
				)
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