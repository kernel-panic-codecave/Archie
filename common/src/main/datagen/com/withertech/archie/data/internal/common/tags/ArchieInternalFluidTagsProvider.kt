package com.withertech.archie.data.internal.common.tags

import com.withertech.archie.Archie
import com.withertech.archie.data.common.tags.ArchieTagsProvider
import com.withertech.archie.data.common.tags.CommonTags
import net.minecraft.core.HolderLookup
import net.minecraft.data.PackOutput
import net.minecraft.world.level.material.Fluids
import java.util.concurrent.CompletableFuture

class ArchieInternalFluidTagsProvider(output: PackOutput, registriesFuture: CompletableFuture<HolderLookup.Provider>) :
	ArchieTagsProvider.FluidTagsProvider(
		output, Archie.MOD,
		registriesFuture,
		false
	)
{
	override fun generate(provider: HolderLookup.Provider)
	{
		CommonTags.Fluids.WATER += listOf(
			Fluids.WATER,
			Fluids.FLOWING_WATER
		)
		CommonTags.Fluids.LAVA += listOf(
			Fluids.LAVA,
			Fluids.FLOWING_LAVA
		)
		CommonTags.Fluids.MILK *= listOf(
			mcLoc("milk"),
			mcLoc("flowing_milk")
		)
		CommonTags.Fluids.GASEOUS()
		CommonTags.Fluids.HONEY()
		CommonTags.Fluids.POTION()
		CommonTags.Fluids.SUSPICIOUS_STEW()
		CommonTags.Fluids.MUSHROOM_STEW()
		CommonTags.Fluids.RABBIT_STEW()
		CommonTags.Fluids.BEETROOT_SOUP()
		CommonTags.Fluids.HIDDEN_FROM_RECIPE_VIEWERS()
	}
}