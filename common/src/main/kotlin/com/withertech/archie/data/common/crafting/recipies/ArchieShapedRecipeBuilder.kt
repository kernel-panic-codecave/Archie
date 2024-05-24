package com.withertech.archie.data.common.crafting.recipies

import net.minecraft.advancements.Criterion
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.level.ItemLike

class ArchieShapedRecipeBuilder : IArchieRecipeBuilder
{


	private val builder: ShapedRecipeBuilder by lazy { ShapedRecipeBuilder(category, result, count) }

	private val rows: MutableList<String> = mutableListOf()
	private val key: MutableMap<Char, Ingredient> = mutableMapOf()
	private val criteria: MutableMap<String, Criterion<*>> = mutableMapOf()

	lateinit var category: RecipeCategory
	lateinit var result: ItemLike
	var count = 1

	var group: String? = null
	var showNotification = true

	fun pattern(block: Pattern.() -> Unit)
	{
		Pattern().apply(block)
	}

	fun pattern(
		vararg lines: String,
	)
	{
		rows.addAll(lines)
	}

	inner class Pattern
	{
		operator fun String.unaryPlus()
		{
			rows.add(this)
		}
	}

	fun key(block: Key.() -> Unit)
	{
		Key().apply(block)
	}

	inner class Key
	{
		infix fun Char.to(tag: TagKey<Item>)
		{
			this to Ingredient.of(tag)
		}

		infix fun Char.to(item: ItemLike)
		{
			this to Ingredient.of(item)
		}

		infix fun Char.to(ingredient: Ingredient)
		{
			require(!key.containsKey(this)) { "Symbol '$this' is already defined!" }
			require(this != ' ') { "Symbol ' ' (whitespace) is reserved and cannot be defined" }
			key[this] = ingredient
		}
	}


	override fun unlockedBy(name: String, criterion: Criterion<*>): ArchieShapedRecipeBuilder
	{
		criteria[name] = criterion
		return this
	}

	override fun group(groupName: String?): ArchieShapedRecipeBuilder
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
		rows.forEach {
			builder.pattern(it)
		}
		key.forEach { (k, v) ->
			builder.define(k, v)
		}
		criteria.forEach { (k, v) ->
			builder.unlockedBy(k, v)
		}
		builder.group(group)
		builder.showNotification(showNotification)

		builder.save(recipeOutput, id)
	}

	companion object
	{
		fun shaped(block: ArchieShapedRecipeBuilder.() -> Unit): ArchieShapedRecipeBuilder
		{
			return ArchieShapedRecipeBuilder().apply(block)
		}
	}
}