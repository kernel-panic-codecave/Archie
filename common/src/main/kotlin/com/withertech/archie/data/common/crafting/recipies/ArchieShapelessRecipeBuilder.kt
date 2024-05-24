package com.withertech.archie.data.common.crafting.recipies

import com.withertech.archie.data.common.tags.CommonTags
import net.minecraft.advancements.Criterion
import net.minecraft.core.NonNullList
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapelessRecipeBuilder
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.level.ItemLike

class ArchieShapelessRecipeBuilder : IArchieRecipeBuilder
{
	private val builder: ShapelessRecipeBuilder by lazy { ShapelessRecipeBuilder(category, result, count) }

	private val ingredients: NonNullList<Ingredient> = NonNullList.create()
	private val criteria: MutableMap<String, Criterion<*>> = mutableMapOf()

	lateinit var category: RecipeCategory
	lateinit var result: ItemLike
	var count = 1

	var group: String? = null

	fun ingredients(block: Ingredients.() -> Unit)
	{
		Ingredients().apply(block)
	}

	inner class Ingredients
	{
		infix fun Int.of(tag: TagKey<Item>)
		{
			check(this >= 1) { "Cannot add less than 1 of ingredient" }
			repeat(this)
			{
				ingredients.add(Ingredient.of(tag))
			}
		}

		infix fun Int.of(item: ItemLike)
		{
			check(this >= 1) { "Cannot add less than 1 of ingredient" }
			repeat(this)
			{
				ingredients.add(Ingredient.of(item))
			}
		}

		infix fun Int.of(ingredient: Ingredient)
		{
			check(this >= 1) { "Cannot add less than 1 of ingredient" }
			repeat(this)
			{
				ingredients.add(ingredient)
			}
		}
	}

	override fun unlockedBy(name: String, criterion: Criterion<*>): ArchieShapelessRecipeBuilder
	{
		criteria[name] = criterion
		return this
	}

	override fun group(groupName: String?): ArchieShapelessRecipeBuilder
	{
		group = groupName
		return this
	}

	override fun getResult(): Item
	{
		check(::category.isInitialized) { "You must specify a recipe category dumbass!" }
		check(::result.isInitialized) { "You must specify a recipe result dumbass!" }
		return builder.result
	}

	override fun save(recipeOutput: RecipeOutput, id: ResourceLocation)
	{
		check(::category.isInitialized) { "You must specify a recipe category dumbass!" }
		check(::result.isInitialized) { "You must specify a recipe result dumbass!" }
		ingredients.forEach {
			builder.requires(it)
		}
		criteria.forEach { (k, v) ->
			builder.unlockedBy(k, v)
		}
		builder.group(group)

		builder.save(recipeOutput, id)
	}

	companion object
	{
		fun shapeless(block: ArchieShapelessRecipeBuilder.() -> Unit): ArchieShapelessRecipeBuilder
		{
			return ArchieShapelessRecipeBuilder().apply(block)
		}
	}
}