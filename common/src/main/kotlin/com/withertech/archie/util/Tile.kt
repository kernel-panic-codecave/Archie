package com.withertech.archie.util

import com.mojang.datafixers.types.templates.Const
import com.mojang.serialization.Codec
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.entity.BlockEntityType.BlockEntitySupplier

fun <T : BlockEntity> BlockEntityType.Builder<T>.build(): BlockEntityType<T>
{
	return build(Const.PrimitiveType(Codec.unit(Unit)))
}

fun <T : BlockEntity> blockEntityType(factory: BlockEntitySupplier<T>, builder: MutableList<Block>.() -> Unit): BlockEntityType<T>
{
	return BlockEntityType.Builder.of(factory, *buildList(builder).toTypedArray()).build()
}