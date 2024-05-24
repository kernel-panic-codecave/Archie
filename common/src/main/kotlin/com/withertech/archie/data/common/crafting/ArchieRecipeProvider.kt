package com.withertech.archie.data.common.crafting

import com.withertech.archie.Archie
import com.withertech.archie.data.IArchieDataProvider
import com.withertech.archie.data.common.conditions.ConditionsPlatform
import com.withertech.archie.data.common.crafting.recipies.ArchieCookingRecipeBuilder
import com.withertech.archie.data.common.crafting.recipies.ArchieShapedRecipeBuilder
import com.withertech.archie.data.common.crafting.recipies.ArchieShapelessRecipeBuilder
import dev.architectury.platform.Mod
import net.minecraft.core.HolderLookup
import net.minecraft.data.CachedOutput
import net.minecraft.data.PackOutput
import net.minecraft.data.recipes.*
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.BlastingRecipe
import net.minecraft.world.item.crafting.CampfireCookingRecipe
import net.minecraft.world.item.crafting.SmeltingRecipe
import net.minecraft.world.item.crafting.SmokingRecipe
import net.minecraft.world.level.ItemLike
import java.util.concurrent.CompletableFuture
import kotlin.system.exitProcess

@Suppress("unused")
abstract class ArchieRecipeProvider(
	override val output: PackOutput,
	override val mod: Mod,
	registries: CompletableFuture<HolderLookup.Provider>,
	override val exitOnError: Boolean
) : RecipeProvider(output, registries),
	IArchieDataProvider
{

	private val fabricParent: RecipeProvider? = ConditionsPlatform.fabricRecipeProvider(this, registries)

	override fun run(cachedOutput: CachedOutput): CompletableFuture<*>
	{
		return if (fabricParent != null)
			fabricParent.run(cachedOutput)
		else
			super.run(cachedOutput)
	}

	final override fun buildRecipes(recipeOutput: RecipeOutput)
	{
		runCatching {
			generate(recipeOutput)
		}.onFailure {
			Archie.LOGGER.error(
				"Data Provider $name failed with exception: ${it.message}\n" +
						"Stacktrace: ${it.stackTraceToString()}"
			)
			if (exitOnError) exitProcess(-1)
		}
	}

	abstract fun generate(recipeOutput: RecipeOutput)

	override fun getName(): String = format("Recipes")


	fun shaped(
		block: ArchieShapedRecipeBuilder.() -> Unit
	): ArchieShapedRecipeBuilder = ArchieShapedRecipeBuilder.shaped(block)

	fun shapeless(
		block: ArchieShapelessRecipeBuilder.() -> Unit
	): ArchieShapelessRecipeBuilder = ArchieShapelessRecipeBuilder.shapeless(block)

	fun smelting(
		block: ArchieCookingRecipeBuilder<SmeltingRecipe>.() -> Unit
	): ArchieCookingRecipeBuilder<SmeltingRecipe> = ArchieCookingRecipeBuilder.smelting(block)

	fun blasting(
		block: ArchieCookingRecipeBuilder<BlastingRecipe>.() -> Unit
	): ArchieCookingRecipeBuilder<BlastingRecipe> = ArchieCookingRecipeBuilder.blasting(block)

	fun smoking(
		block: ArchieCookingRecipeBuilder<SmokingRecipe>.() -> Unit
	): ArchieCookingRecipeBuilder<SmokingRecipe> = ArchieCookingRecipeBuilder.smoking(block)

	fun cooking(
		block: ArchieCookingRecipeBuilder<CampfireCookingRecipe>.() -> Unit
	): ArchieCookingRecipeBuilder<CampfireCookingRecipe> = ArchieCookingRecipeBuilder.cooking(block)

	@Suppress("UNCHECKED_CAST")
	fun <T : RecipeBuilder> T.unlockedBy(ingredient: ItemLike): T =
		unlockedBy(getHasName(ingredient), has(ingredient)) as T

	@Suppress("UNCHECKED_CAST")
	fun <T : RecipeBuilder> T.unlockedBy(tag: TagKey<Item>): T =
		unlockedBy("has_${tag.location.path}", has(tag)) as T

}