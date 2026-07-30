package net.kernelpanicsoft.archie.data.common.crafting.ingredients

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.util.rem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import java.util.*

/** Custom ingredient that matches a stack when at least one of its sub-ingredients matches it. Build via [of]. */
class AAnyIngredient private constructor(ingredients: List<Ingredient>): ACombinedIngredient(ingredients)
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
	override val serializer: IACustomIngredientSerializer<*> = Serializer

	companion object
	{
		/** Creates a vanilla [Ingredient] that matches when at least one of [ingredients] matches. */
		fun of(vararg ingredients: Ingredient): Ingredient = AAnyIngredient(ingredients.toList()).vanilla
		private val ALLOW_EMPTY_CODEC = createCodec(Ingredient.CODEC)
		private val DISALLOW_EMPTY_CODEC = createCodec(Ingredient.CODEC_NONEMPTY)

		private fun createCodec(ingredientCodec: Codec<Ingredient>): MapCodec<AAnyIngredient>
		{
			return ingredientCodec
				.listOf()
				.fieldOf("ingredients")
				.xmap(::AAnyIngredient, AAnyIngredient::ingredients)
		}

		val Serializer: IACustomIngredientSerializer<AAnyIngredient> =
			Serializer(
				Archie % "any",
				::AAnyIngredient, ALLOW_EMPTY_CODEC, DISALLOW_EMPTY_CODEC
			)
	}
}