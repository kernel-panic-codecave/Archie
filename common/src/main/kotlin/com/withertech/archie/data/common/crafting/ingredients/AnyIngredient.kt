package com.withertech.archie.data.common.crafting.ingredients

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.withertech.archie.Archie
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import java.util.*

class AnyIngredient private constructor(ingredients: List<Ingredient>): CombinedIngredient(ingredients)
{
	override fun test(stack: ItemStack): Boolean
	{
		return ingredients.any { ingredient -> ingredient.test(stack) }
	}

	override val matchingStacks: MutableList<ItemStack> by lazy {
		val previewStacks: MutableList<ItemStack> = ArrayList()
		for (ingredient in ingredients)
		{
			previewStacks.addAll(listOf(*ingredient.items))
		}

		previewStacks
	}
	override val serializer: ICustomIngredientSerializer<*> = Serializer

	companion object
	{
		fun of(vararg ingredients: Ingredient): Ingredient = AnyIngredient(ingredients.toList()).vanilla
		private val ALLOW_EMPTY_CODEC = createCodec(Ingredient.CODEC)
		private val DISALLOW_EMPTY_CODEC = createCodec(Ingredient.CODEC_NONEMPTY)

		private fun createCodec(ingredientCodec: Codec<Ingredient>): MapCodec<AnyIngredient>
		{
			return ingredientCodec
				.listOf()
				.fieldOf("ingredients")
				.xmap(::AnyIngredient, AnyIngredient::ingredients)
		}

		val Serializer: ICustomIngredientSerializer<AnyIngredient> =
			Serializer(
				Archie["any"],
				::AnyIngredient, ALLOW_EMPTY_CODEC, DISALLOW_EMPTY_CODEC
			)
	}
}