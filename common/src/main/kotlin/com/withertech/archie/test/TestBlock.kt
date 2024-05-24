package com.withertech.archie.test

import com.mojang.serialization.MapCodec
import dev.architectury.registry.menu.ExtendedMenuProvider
import dev.architectury.registry.menu.MenuRegistry
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

class TestBlock(properties: Properties) : BaseEntityBlock(properties)
{
	override fun codec(): MapCodec<out BaseEntityBlock> = CODEC

	override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
		TileRegistry.TestTile.create(pos, state)

	override fun getRenderShape(state: BlockState): RenderShape
	{
		return RenderShape.MODEL
	}

	override fun useWithoutItem(
		blockState: BlockState,
		level: Level,
		blockPos: BlockPos,
		player: Player,
		blockHitResult: BlockHitResult
	): InteractionResult
	{
		if (!level.isClientSide)
		{
			val tile = level.getBlockEntity(blockPos)
			if (tile is ExtendedMenuProvider)
			{
				MenuRegistry.openExtendedMenu(player as ServerPlayer, tile)
			}
		}
		return InteractionResult.sidedSuccess(level.isClientSide)
	}

	companion object
	{
		val CODEC = simpleCodec(::TestBlock)
	}
}