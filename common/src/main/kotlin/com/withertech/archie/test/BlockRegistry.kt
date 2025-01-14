package com.withertech.archie.test

import com.withertech.archie.Archie
import com.withertech.archie.registries.ADeferredRegistryHolder
import com.withertech.archie.util.blockProperties
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks

object BlockRegistry : ADeferredRegistryHolder<Block>(Archie.MOD, Registries.BLOCK)
{
	val TestBlock: TestBlock by register("test_block")  {
		TestBlock(blockProperties(Blocks.COBBLESTONE) {

		})
	}
}