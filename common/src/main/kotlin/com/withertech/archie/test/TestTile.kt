package com.withertech.archie.test

import com.withertech.archie.block.entity.NBTBlockEntity
import dev.architectury.registry.menu.ExtendedMenuProvider
import earth.terrarium.common_storage_lib.item.util.ItemProvider
import earth.terrarium.common_storage_lib.resources.item.ItemResource
import earth.terrarium.common_storage_lib.storage.base.CommonStorage
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.level.block.state.BlockState

class TestTile(pos: BlockPos, blockState: BlockState) : NBTBlockEntity(TileRegistry.TestTile, pos, blockState), ExtendedMenuProvider, ItemProvider.BlockEntity
{
	val test by intField()

	val items by itemField(27 * 2)

	override fun getItems(direction: Direction?): CommonStorage<ItemResource>
	{
		return items
	}

	override fun createMenu(i: Int, inventory: Inventory, player: Player): AbstractContainerMenu
	{
		return TestMenu(i, inventory, this)
	}

	override fun getDisplayName(): Component
	{
		return blockState.block.name
	}

	override fun saveExtraData(buf: FriendlyByteBuf)
	{
		buf.writeBlockPos(blockPos)
	}
}