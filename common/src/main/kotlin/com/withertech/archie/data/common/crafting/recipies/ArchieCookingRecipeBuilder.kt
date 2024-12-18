package com.withertech.archie.data.common.crafting.recipies

import net.minecraft.advancements.Criterion
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.AbstractCookingRecipe
import net.minecraft.world.item.crafting.BlastingRecipe
import net.minecraft.world.item.crafting.CampfireCookingRecipe
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.SmeltingRecipe
import net.minecraft.world.item.crafting.SmokingRecipe
import net.minecraft.world.level.ItemLike
import kotlin.properties.Delegates

class ArchieCookingRecipeBuilder<T : AbstractCookingRecipe>(
	private val factory: AbstractCookingRecipe.Factory<T>,
	private val serializer: RecipeSerializer<T>
) : IARecipeBuilder
{
	private val builder: SimpleCookingRecipeBuilder by lazy {
		SimpleCookingRecipeBuilder.generic(
			ingredient,
			category,
			result,
			experience,
			cookingTime,
			serializer,
			factory
		)
	}
	private val criteria: MutableMap<String, Criterion<*>> = mutableMapOf()

	lateinit var category: RecipeCategory
	lateinit var result: ItemLike
	lateinit var ingredient: Ingredient
	var experience by Delegates.notNull<Float>()
	var cookingTime by Delegates.notNull<Int>()

	var group: String? = null

	private fun checkVars()
	{
		check(::category.isInitialized) { "You must specify a recipe category dumbass!" }
		check(::result.isInitialized) { "You must specify a recipe result dumbass!" }
		check(::ingredient.isInitialized) { "You must specify a recipe ingredient dumbass!" }
		check(runCatching { experience }.isSuccess) { "You must specify an experience amount dumbass!" }
		check(runCatching { cookingTime }.isSuccess) { "You must specify a cooking time dumbass!" }
	}

	override fun unlockedBy(name: String, criterion: Criterion<*>): ArchieCookingRecipeBuilder<T>
	{
		criteria[name] = criterion
		return this
	}

	override fun group(groupName: String?): ArchieCookingRecipeBuilder<T>
	{
		group = groupName
		return this
	}

	override fun getResult(): Item
	{
		checkVars()
		return builder.result
	}

	override fun save(recipeOutput: RecipeOutput, id: ResourceLocation)
	{
		checkVars()
		criteria.forEach { (k, v) ->
			builder.unlockedBy(k, v)
		}
		builder.group(group)

		builder.save(recipeOutput, id)
	}

	companion object
	{
		fun smelting(block: ArchieCookingRecipeBuilder<SmeltingRecipe>.() -> Unit): ArchieCookingRecipeBuilder<SmeltingRecipe> =
			ArchieCookingRecipeBuilder<SmeltingRecipe>(::SmeltingRecipe, RecipeSerializer.SMELTING_RECIPE).apply(block)

		fun blasting(block: ArchieCookingRecipeBuilder<BlastingRecipe>.() -> Unit): ArchieCookingRecipeBuilder<BlastingRecipe> =
			ArchieCookingRecipeBuilder<BlastingRecipe>(::BlastingRecipe, RecipeSerializer.BLASTING_RECIPE).apply(block)

		fun smoking(block: ArchieCookingRecipeBuilder<SmokingRecipe>.() -> Unit): ArchieCookingRecipeBuilder<SmokingRecipe> =
			ArchieCookingRecipeBuilder<SmokingRecipe>(::SmokingRecipe, RecipeSerializer.SMOKING_RECIPE).apply(block)

		fun cooking(block: ArchieCookingRecipeBuilder<CampfireCookingRecipe>.() -> Unit): ArchieCookingRecipeBuilder<CampfireCookingRecipe> =
			ArchieCookingRecipeBuilder<CampfireCookingRecipe>(::CampfireCookingRecipe, RecipeSerializer.CAMPFIRE_COOKING_RECIPE).apply(block)
	}

}