package net.kernelpanicsoft.archie.data.internal.common.tags

import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.data.common.tags.ATagsProvider
import net.kernelpanicsoft.archie.data.common.tags.ACommonTags
import net.minecraft.core.HolderLookup
import net.minecraft.data.PackOutput
import net.minecraft.world.level.material.Fluids
import java.util.concurrent.CompletableFuture

/**
 * Populates Archie's vanilla-derived common ("c") fluid tags (see [ACommonTags.Fluids]) with
 * their vanilla fluid members, so downstream mods can depend on the `c` tag convention without
 * every mod having to redeclare it.
 */
class AInternalFluidTagsProvider(output: PackOutput, registriesFuture: CompletableFuture<HolderLookup.Provider>) :
	ATagsProvider.FluidTagsProvider(
		output, Archie.MOD,
		registriesFuture,
		false
	)
{
	override fun generate(registries: HolderLookup.Provider)
	{
		ACommonTags.Fluids.WATER += listOf(
			Fluids.WATER,
			Fluids.FLOWING_WATER
		)
		ACommonTags.Fluids.LAVA += listOf(
			Fluids.LAVA,
			Fluids.FLOWING_LAVA
		)
		ACommonTags.Fluids.MILK *= listOf(
			mcLoc("milk"),
			mcLoc("flowing_milk")
		)
		ACommonTags.Fluids.GASEOUS()
		ACommonTags.Fluids.HONEY()
		ACommonTags.Fluids.POTION()
		ACommonTags.Fluids.SUSPICIOUS_STEW()
		ACommonTags.Fluids.MUSHROOM_STEW()
		ACommonTags.Fluids.RABBIT_STEW()
		ACommonTags.Fluids.BEETROOT_SOUP()
		ACommonTags.Fluids.HIDDEN_FROM_RECIPE_VIEWERS()
	}
}