package com.withertech.archie.test

import com.withertech.archie.test.container.ArchieContainerMenu
import net.minecraft.world.entity.player.Inventory

class TestMenu(id: Int, inventory: Inventory, tile: TestTile) : ArchieContainerMenu<TestTile, TestMenu>(GuiRegistry.TestMenu, id, inventory, tile)
{
	val rows: Int = tile.items.size() / 9

	override val playerXOffset: Int = 8
	override val playerYOffset: Int = 103 + ((rows - 4) * 18)


	override fun addMenuSlots()
	{
		slotGrid(8, 18, 9, rows, tile.items)
	}

	init
	{
		addSlots()
	}
}