package net.kernelpanicsoft.archie.data.common.crafting.ingredients

import net.minecraft.world.item.crafting.Ingredient

/** Cross-loader hook converting an [IACustomIngredient] into a vanilla [Ingredient]; backs [IACustomIngredient.vanilla]. */
internal expect object ACustomIngredientPlatform
{
	fun vanillaOf(custom: IACustomIngredient): Ingredient
}