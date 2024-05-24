package com.withertech.archie.test

import com.withertech.archie.Archie
import com.withertech.archie.registries.DeferredRegistryHolder
import com.withertech.archie.util.blockEntityType
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.block.entity.BlockEntityType

object TileRegistry : DeferredRegistryHolder<BlockEntityType<*>>(Archie.MOD, Registries.BLOCK_ENTITY_TYPE)
{
	val TestTile: BlockEntityType<TestTile> by register("test_tile") {
		blockEntityType(::TestTile) {
			add(BlockRegistry.TestBlock)
		}
	}
}