package com.withertech.archie.data.common.conditions

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.withertech.archie.data.common.crafting.ArchieRecipeProvider
import dev.architectury.injectables.annotations.ExpectPlatform
import net.minecraft.core.HolderLookup
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.RecipeProvider
import net.minecraft.resources.ResourceLocation
import java.util.concurrent.CompletableFuture

object ConditionsPlatform
{
	@ExpectPlatform
	@JvmStatic
	fun register(identifier: ResourceLocation, codec: MapCodec<out ICondition>)
	{
		throw AssertionError()
	}

	@ExpectPlatform
	@JvmStatic
	fun withCondition(output: RecipeOutput, condition: ICondition): RecipeOutput
	{
		throw AssertionError()
	}

	@ExpectPlatform
	@JvmStatic
	fun codec(): Codec<ICondition>
	{
		throw AssertionError()
	}

	@ExpectPlatform
	@JvmStatic
	fun fabricRecipeProvider(child: ArchieRecipeProvider, registries: CompletableFuture<HolderLookup.Provider>): RecipeProvider?
	{
		throw AssertionError()
	}
}