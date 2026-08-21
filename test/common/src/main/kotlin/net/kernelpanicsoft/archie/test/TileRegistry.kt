package net.kernelpanicsoft.archie.test

import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.registries.ADeferredRegistryHolder
import net.kernelpanicsoft.archie.transfer.exposeItemStorage
import net.kernelpanicsoft.archie.util.blockEntityType
import net.minecraft.core.Direction
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.block.entity.BlockEntityType

object TileRegistry : ADeferredRegistryHolder<BlockEntityType<*>>(Archie.MOD, Registries.BLOCK_ENTITY_TYPE)
{
	val TestTile: BlockEntityType<TestTile> by register("test_tile") {
		blockEntityType(::TestTile) {
			add(BlockRegistry.TestBlock)
		}
	}.apply {
		exposeItemStorage { tile, direction ->
			if (direction == Direction.DOWN) null else tile.items
		}
	}
}