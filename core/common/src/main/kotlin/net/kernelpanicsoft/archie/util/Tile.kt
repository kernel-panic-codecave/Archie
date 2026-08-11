package net.kernelpanicsoft.archie.util

import com.mojang.datafixers.types.templates.Const
import com.mojang.serialization.Codec
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.entity.BlockEntityType.BlockEntitySupplier

/**
 * Builds a [BlockEntityType] for [factory], valid for the set of [Block]s declared via
 * [builder] (e.g. `{ add(MyBlocks.MY_BLOCK.get()) }`).
 */
fun <T : BlockEntity> blockEntityType(factory: BlockEntitySupplier<T>, builder: MutableList<Block>.() -> Unit): BlockEntityType<T>
{
	return BlockEntityType.Builder.of(factory, *buildList(builder).toTypedArray())
		.build(Const.PrimitiveType(Codec.unit(Unit)))
}