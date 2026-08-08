package net.kernelpanicsoft.archie.test

import net.kernelpanicsoft.archie.gui.item.ComposeItemContainerMenu
import net.kernelpanicsoft.archie.gui.item.ItemContainerAccess
import net.kernelpanicsoft.archie.serialization.Sync
import net.minecraft.world.entity.player.Inventory

/** Fixture item-backed menu for [net.kernelpanicsoft.archie.test.gametest.ComposeItemContainerMenuTests]/[net.kernelpanicsoft.archie.test.gametest.ComposeItemContainerMenuClientTests]. */
class TestItemMenu(id: Int, inventory: Inventory, access: ItemContainerAccess) :
	ComposeItemContainerMenu<TestItemMenu>(GuiRegistry.TestItemMenu, id, inventory, access)
{
	@Sync
	var counter by holder.intField()

	val items by holder.itemField(9)

	override fun registerSlotHandlers()
	{
		handler("inventory", items)
	}
}
