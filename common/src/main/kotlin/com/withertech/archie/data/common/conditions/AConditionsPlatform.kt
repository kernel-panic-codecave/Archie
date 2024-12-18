package com.withertech.archie.data.common.conditions

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.withertech.archie.data.common.crafting.ARecipeProvider
import net.minecraft.core.HolderLookup
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.RecipeProvider
import net.minecraft.resources.ResourceLocation
import java.util.concurrent.CompletableFuture

expect object AConditionsPlatform
{
	fun register(identifier: ResourceLocation, codec: MapCodec<out IACondition>)

	fun withCondition(output: RecipeOutput, condition: IACondition): RecipeOutput

	fun codec(): Codec<IACondition>

	fun fabricRecipeProvider(child: ARecipeProvider, registries: CompletableFuture<HolderLookup.Provider>): RecipeProvider?
}