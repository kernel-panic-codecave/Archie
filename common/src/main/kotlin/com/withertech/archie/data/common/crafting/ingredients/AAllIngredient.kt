package com.withertech.archie.data.common.crafting.ingredients

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.withertech.archie.Archie
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient

class AAllIngredient private constructor(ingredients: List<Ingredient>) :
	ACombinedIngredient(ingredients)
{
	override fun test(stack: ItemStack): Boolean
	{
		return ingredients.all { ingredient -> ingredient.test(stack) }
	}

	override val matchingStacks: MutableList<ItemStack> by lazy {
			// There's always at least one sub ingredient, so accessing ingredients[0] is safe.
			val previewStacks: MutableList<ItemStack> =
				mutableListOf(*ingredients[0].items)

			for (i in 1 until ingredients.size)
			{
				val ing: Ingredient = ingredients[i]
				previewStacks.removeIf { stack: ItemStack ->
					!ing.test(
						stack
					)
				}
			}

			previewStacks
		}

	override val serializer: IACustomIngredientSerializer<*> = Serializer

	companion object
	{
		fun of(vararg ingredients: Ingredient): Ingredient = AAllIngredient(ingredients.toList()).vanilla
		private val ALLOW_EMPTY_CODEC = createCodec(Ingredient.CODEC)
		private val DISALLOW_EMPTY_CODEC = createCodec(Ingredient.CODEC_NONEMPTY)

		private fun createCodec(ingredientCodec: Codec<Ingredient>): MapCodec<AAllIngredient>
		{
			return ingredientCodec
				.listOf()
				.fieldOf("ingredients")
				.xmap(::AAllIngredient, AAllIngredient::ingredients)
		}

		val Serializer: IACustomIngredientSerializer<AAllIngredient> =
			Serializer(
				Archie["all"],
				::AAllIngredient, ALLOW_EMPTY_CODEC, DISALLOW_EMPTY_CODEC
			)
	}
}