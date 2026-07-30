package net.kernelpanicsoft.archie.test

import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.registries.ADeferredRegistryHolder
import dev.architectury.registry.menu.MenuRegistry
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.MenuType

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

	override fun initClient()
	{
		MenuRegistry.registerScreenFactory(TestMenu, ::TestScreen)
	}

}