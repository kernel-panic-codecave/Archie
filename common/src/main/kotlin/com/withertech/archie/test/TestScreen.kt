package com.withertech.archie.test

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import com.withertech.archie.gui.*
import com.withertech.archie.gui.layout.*
import com.withertech.archie.gui.modifiers.Constraints
import com.withertech.archie.gui.modifiers.Modifier
import com.withertech.archie.gui.modifiers.fillMaxSize
import com.withertech.archie.gui.nodes.AUINodeApplier
import kotlinx.coroutines.*
import net.fabricmc.loader.impl.launch.knot.Knot.launch
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.coroutines.CoroutineContext

class TestScreen(menu: TestMenu, playerInventory: Inventory, title: Component) : ComposeContainerScreen<TestMenu>(menu, playerInventory,
	title
)
{
//	private val rows = menu.rows

	init
	{
//		this.imageHeight = 114 + rows * 18
//		this.inventoryLabelY = this.imageHeight - 94
		start {
			content()
		}
	}

	@Composable
	fun content()
	{
		Column {
			for (i in 0 until menu.rows)
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