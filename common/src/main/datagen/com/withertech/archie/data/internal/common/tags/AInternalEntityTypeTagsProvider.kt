package com.withertech.archie.data.internal.common.tags

import com.withertech.archie.Archie
import com.withertech.archie.data.common.tags.ATagsProvider
import com.withertech.archie.data.common.tags.ACommonTags
import net.minecraft.core.HolderLookup
import net.minecraft.data.PackOutput
import net.minecraft.world.entity.EntityType
import java.util.concurrent.CompletableFuture

class  AInternalEntityTypeTagsProvider(
	output: PackOutput,
	registriesFuture: CompletableFuture<HolderLookup.Provider>
) : ATagsProvider.EntityTypeTagsProvider(output, Archie.MOD, registriesFuture, false)
{
	override fun generate(registries: HolderLookup.Provider)
	{
		ACommonTags.EntityTypes.BOSSES += listOf(
			EntityType.ENDER_DRAGON,
			EntityType.WITHER
		)
		ACommonTags.EntityTypes.MINECARTS += listOf(
			EntityType.MINECART,
			EntityType.CHEST_MINECART,
			EntityType.FURNACE_MINECART,
			EntityType.HOPPER_MINECART,
			EntityType.SPAWNER_MINECART,
			EntityType.TNT_MINECART,
			EntityType.COMMAND_BLOCK_MINECART
		)
		ACommonTags.EntityTypes.BOATS += listOf(
			EntityType.BOAT,
			EntityType.CHEST_BOAT
		)
		ACommonTags.EntityTypes.CAPTURING_NOT_SUPPORTED()
		ACommonTags.EntityTypes.TELEPORTING_NOT_SUPPORTED()
	}
}