package net.kernelpanicsoft.archie.test

import net.kernelpanicsoft.archie.gui.ComposeContainerMenu
import net.minecraft.world.entity.player.Inventory

class TestMenu(id: Int, inventory: Inventory, tile: TestTile) : ComposeContainerMenu<TestTile, TestMenu>(GuiRegistry.TestMenu, id, inventory, tile)
{
	val rows: Int = tile.items.size() / 9

	override fun registerSlotHandlers()
	{
		handler("inventory", tile.items)
	}

}