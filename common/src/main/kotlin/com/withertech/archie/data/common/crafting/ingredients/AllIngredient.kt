package com.withertech.archie.data.common.crafting.ingredients

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.withertech.archie.Archie
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient

class AllIngredient private constructor(ingredients: List<Ingredient>) :
	CombinedIngredient(ingredients)
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

	override val serializer: ICustomIngredientSerializer<*> = Serializer

	companion object
	{
		fun of(vararg ingredients: Ingredient): Ingredient = AllIngredient(ingredients.toList()).vanilla
		private val ALLOW_EMPTY_CODEC = createCodec(Ingredient.CODEC)
		private val DISALLOW_EMPTY_CODEC = createCodec(Ingredient.CODEC_NONEMPTY)

		private fun createCodec(ingredientCodec: Codec<Ingredient>): MapCodec<AllIngredient>
		{
			return ingredientCodec
				.listOf()
				.fieldOf("ingredients")
				.xmap(::AllIngredient, AllIngredient::ingredients)
		}

		val Serializer: ICustomIngredientSerializer<AllIngredient> =
			Serializer(
				Archie["all"],
				::AllIngredient, ALLOW_EMPTY_CODEC, DISALLOW_EMPTY_CODEC
			)
	}
}