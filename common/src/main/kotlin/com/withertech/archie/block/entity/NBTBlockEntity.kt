package com.withertech.archie.block.entity

import com.withertech.archie.serialization.NBTHolder
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

abstract class NBTBlockEntity(type: BlockEntityType<*>, pos: BlockPos, blockState: BlockState) : BlockEntity(
	type, pos,
	blockState
), NBTHolder by NBTHolder.create()
{
	override fun loadAdditional(compoundTag: CompoundTag, provider: HolderLookup.Provider)
	{
		super.loadAdditional(compoundTag, provider)
		loadFromTag(compoundTag)
	}

	override fun saveAdditional(compoundTag: CompoundTag, provider: HolderLookup.Provider)
	{
		super.saveAdditional(compoundTag, provider)
		saveToTag(compoundTag)
	}

	override fun getUpdateTag(provider: HolderLookup.Provider): CompoundTag
	{
		return getSyncTag()
	}

	override fun getUpdatePacket(): Packet<ClientGamePacketListener>?
	{
		return ClientboundBlockEntityDataPacket.create(this)
	}
}