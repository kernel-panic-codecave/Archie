package net.kernelpanicsoft.archie.test

import net.kernelpanicsoft.archie.gui.ComposeBlockContainerMenu
import net.minecraft.world.entity.player.Inventory

class TestMenu(id: Int, inventory: Inventory, tile: TestTile) : ComposeBlockContainerMenu<TestTile, TestMenu>(GuiRegistry.TestMenu, id, inventory, tile)
{
	val rows: Int = tile.items.size() / 9

	override fun registerSlotHandlers()
	{
		for (showcase in TestKind.entries)
			handler("inventory_" + showcase.name.lowercase(), tile.items)
	}

}