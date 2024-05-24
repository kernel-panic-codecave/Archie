package com.withertech.archie.test

import com.withertech.archie.Archie
import com.withertech.archie.registries.DeferredRegistryHolder
import com.withertech.archie.util.itemProperties
import com.withertech.archie.util.tab
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item

object ItemRegistry : DeferredRegistryHolder<Item>(Archie.MOD, Registries.ITEM)
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