package net.kernelpanicsoft.archie.test

import net.kernelpanicsoft.archie.gui.ComposeContainerScreen
import net.kernelpanicsoft.archie.gui.PlayerSlots
import net.kernelpanicsoft.archie.gui.Slots
import net.kernelpanicsoft.archie.gui.theme.Theme
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

/** Minimal fixture screen for [TestItemMenu] - just enough layout to drive a real [net.kernelpanicsoft.archie.gui.ComposeContainerMenuBase.updateSlotData] round trip. */
class TestItemContainerScreen(menu: TestItemMenu, playerInventory: Inventory, title: Component) : ComposeContainerScreen<TestItemMenu>(menu, playerInventory, title)
{
	init
	{
		start {
			Theme {
				Slots("inventory", 9, 1)
				PlayerSlots()
			}
		}
	}
}
