package com.withertech.archie.data.internal.common.tags

import com.withertech.archie.Archie
import com.withertech.archie.data.common.tags.ArchieTagsProvider
import com.withertech.archie.data.common.tags.CommonTags
import net.minecraft.core.HolderLookup
import net.minecraft.data.PackOutput
import net.minecraft.world.entity.EntityType
import java.util.concurrent.CompletableFuture

class  ArchieInternalEntityTypeTagsProvider(
	output: PackOutput,
	registriesFuture: CompletableFuture<HolderLookup.Provider>
) : ArchieTagsProvider.EntityTypeTagsProvider(output, Archie.MOD, registriesFuture, false)
{
	override fun generate(provider: HolderLookup.Provider)
	{
		CommonTags.EntityTypes.BOSSES += listOf(
			EntityType.ENDER_DRAGON,
			EntityType.WITHER
		)
		CommonTags.EntityTypes.MINECARTS += listOf(
			EntityType.MINECART,
			EntityType.CHEST_MINECART,
			EntityType.FURNACE_MINECART,
			EntityType.HOPPER_MINECART,
			EntityType.SPAWNER_MINECART,
			EntityType.TNT_MINECART,
			EntityType.COMMAND_BLOCK_MINECART
		)
		CommonTags.EntityTypes.BOATS += listOf(
			EntityType.BOAT,
			EntityType.CHEST_BOAT
		)
		CommonTags.EntityTypes.CAPTURING_NOT_SUPPORTED()
		CommonTags.EntityTypes.TELEPORTING_NOT_SUPPORTED()
	}
}