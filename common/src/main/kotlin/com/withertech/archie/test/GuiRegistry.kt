package com.withertech.archie.test

import com.withertech.archie.Archie
import com.withertech.archie.registries.DeferredRegistryHolder
import dev.architectury.injectables.annotations.ExpectPlatform
import dev.architectury.registry.menu.MenuRegistry
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.MenuAccess
import net.minecraft.core.registries.Registries
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.MenuType

object GuiRegistry : DeferredRegistryHolder<MenuType<*>>(Archie.MOD, Registries.MENU)
{
	val TestMenu: MenuType<TestMenu> by register("test_menu") {
		MenuRegistry.ofExtended { id, inventory, buf -> TestMenu(id, inventory, TileRegistry.TestTile.getBlockEntity(inventory.player.level(), buf.readBlockPos())!!) }
	}

	fun initClient()
	{
		MenuRegistry.registerScreenFactory(TestMenu, ::TestScreen)
	}

}