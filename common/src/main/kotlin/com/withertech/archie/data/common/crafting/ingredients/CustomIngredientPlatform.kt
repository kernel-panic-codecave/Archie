package com.withertech.archie.data.common.crafting.ingredients

import dev.architectury.injectables.annotations.ExpectPlatform
import net.minecraft.world.item.crafting.Ingredient

internal object CustomIngredientPlatform
{
	@ExpectPlatform
	@JvmStatic
	fun vanillaOf(custom: ICustomIngredient): Ingredient
	{
		throw AssertionError()
	}
}