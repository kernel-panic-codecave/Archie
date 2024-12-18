package com.withertech.archie.data.common.crafting.ingredients

import com.withertech.archie.data.common.crafting.ingredients.ACustomIngredientSerializerPlatform.fabric
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient

actual object ACustomIngredientPlatform
{
	actual fun vanillaOf(custom: IACustomIngredient): Ingredient
	{
		return custom.fabric.toVanilla()
	}

	val <T : IACustomIngredient> T.fabric: CustomIngredient
		get() = FabricCustomIngredient(this)

	class FabricCustomIngredient<T : IACustomIngredient>
		(
		override val custom: T
	) : CustomIngredient, IACustomIngredientHolder<T>
	{
		override fun test(stack: ItemStack): Boolean
		{
			return custom.test(stack)
		}

		override fun getMatchingStacks(): MutableList<ItemStack>
		{
			return custom.matchingStacks
		}

		override fun requiresTesting(): Boolean
		{
			return custom.requiresTesting
		}

		override fun getSerializer(): CustomIngredientSerializer<*>
		{
			return custom.serializer.fabric
		}
	}
}