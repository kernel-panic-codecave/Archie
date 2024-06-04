package com.withertech.archie.data.common.conditions

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.withertech.archie.data.common.crafting.ArchieRecipeProvider
import net.minecraft.core.HolderLookup
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.RecipeProvider
import net.minecraft.resources.ResourceLocation
import java.util.concurrent.CompletableFuture

expect object ConditionsPlatform
{
	fun register(identifier: ResourceLocation, codec: MapCodec<out ICondition>)

	fun withCondition(output: RecipeOutput, condition: ICondition): RecipeOutput

	fun codec(): Codec<ICondition>

	fun fabricRecipeProvider(child: ArchieRecipeProvider, registries: CompletableFuture<HolderLookup.Provider>): RecipeProvider?
}