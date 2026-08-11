package net.kernelpanicsoft.archie.test

import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.registries.ADeferredRegistryHolder
import net.kernelpanicsoft.archie.util.blockProperties
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