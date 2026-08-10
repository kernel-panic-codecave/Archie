package net.kernelpanicsoft.archie.block.entity

import net.kernelpanicsoft.archie.serialization.NBTHolder
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

/**
 * A [BlockEntity] base class that automatically persists fields declared with [NBTHolder]
 * delegates to and from the block entity's [CompoundTag].
 *
 * Subclass this and declare fields using the [NBTHolder] delegation API:
 * ```kotlin
 * class MyBlockEntity(pos: BlockPos, state: BlockState)
 *     : NBTBlockEntity(MY_TYPE, pos, state) {
 *
 *     var energy by nbt.intField()
 *     var label  by nbt.stringField { "default" }
 *     val items  by nbt.itemField(9)
 * }
 * ```
 *
 * Saving and loading are handled automatically via [saveAdditional] and [loadAdditional].
 * Call [BlockEntity.setChanged] to push the block entity state to tracking clients.
 */
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

	/** Returns the [NBTHolder] sync tag sent to tracking clients; see [NBTHolder.getSyncTag]. */
	override fun getUpdateTag(provider: HolderLookup.Provider): CompoundTag
	{
		return getSyncTag()
	}

	/** Builds the block entity update packet carrying [getUpdateTag]'s data. */
	override fun getUpdatePacket(): Packet<ClientGamePacketListener>?
	{
		return ClientboundBlockEntityDataPacket.create(this)
	}
}