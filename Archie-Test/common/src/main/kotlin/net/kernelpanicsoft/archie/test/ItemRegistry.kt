package net.kernelpanicsoft.archie.test

import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.registries.ADeferredRegistryHolder
import net.kernelpanicsoft.archie.util.itemProperties
import net.kernelpanicsoft.archie.util.tab
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item

object ItemRegistry : ADeferredRegistryHolder<Item>(Archie.MOD, Registries.ITEM)
{
	val TestBlock by register("test_block") {
		BlockItem(BlockRegistry.TestBlock, itemProperties {
			tab(CreativeModeTabs.TOOLS_AND_UTILITIES)
		})
	}

	val TestItem by register("test_item") {
		TestItem(itemProperties {
			tab(CreativeModeTabs.TOOLS_AND_UTILITIES)
		})
	}
}