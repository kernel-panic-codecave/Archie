package net.kernelpanicsoft.archie.test

import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.gui.item.PlayerInventoryItemAccess
import net.kernelpanicsoft.archie.registries.ADeferredRegistryHolder
import dev.architectury.registry.menu.MenuRegistry
import net.kernelpanicsoft.archie.util.onClient
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.Items

object GuiRegistry : ADeferredRegistryHolder<MenuType<*>>(Archie.MOD, Registries.MENU)
{
	val TestMenu: MenuType<TestMenu> by register("test_menu") {
		MenuRegistry.ofExtended { id, inventory, buf ->
			TestMenu(
				id,
				inventory,
				TileRegistry.TestTile.getBlockEntity(inventory.player.level(), buf.readBlockPos())!!
			)
		}
	}

	val TestItemMenu: MenuType<TestItemMenu> by register("test_item_menu") {
		MenuRegistry.ofExtended { id, inventory, buf ->
			TestItemMenu(
				id,
				inventory,
				PlayerInventoryItemAccess(inventory.player, buf.readVarInt(), Items.PAPER)
			)
		}
	}

	override fun init()
	{
		super.init()
		listen {
			onClient {
				MenuRegistry.registerScreenFactory(TestMenu, ::TestScreen)
				MenuRegistry.registerScreenFactory(TestItemMenu, ::TestItemContainerScreen)
			}
		}
	}

}