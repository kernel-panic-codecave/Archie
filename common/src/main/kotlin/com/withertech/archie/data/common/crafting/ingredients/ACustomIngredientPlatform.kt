package com.withertech.archie.data.common.crafting.ingredients

import net.minecraft.world.item.crafting.Ingredient

internal expect object ACustomIngredientPlatform
{
	fun vanillaOf(custom: IACustomIngredient): Ingredient
}