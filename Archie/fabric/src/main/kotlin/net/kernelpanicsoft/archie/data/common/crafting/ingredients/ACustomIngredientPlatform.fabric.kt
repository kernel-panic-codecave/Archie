package net.kernelpanicsoft.archie.data.common.crafting.ingredients

import net.kernelpanicsoft.archie.data.common.crafting.ingredients.ACustomIngredientSerializerPlatform.fabric
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient

/** Fabric implementation of [ACustomIngredientPlatform], adapting [IACustomIngredient] onto Fabric's `CustomIngredient` API. */
actual object ACustomIngredientPlatform
{
	/** Wraps [custom] as a Fabric [CustomIngredient] and converts it to a vanilla [Ingredient]. */
	actual fun vanillaOf(custom: IACustomIngredient): Ingredient
	{
		return custom.fabric.toVanilla()
	}

	/** Wraps this ingredient as a Fabric [CustomIngredient]. */
	val <T : IACustomIngredient> T.fabric: CustomIngredient
		get() = FabricCustomIngredient(this)

	/** Adapts an [IACustomIngredient] to Fabric's [CustomIngredient] interface, delegating all matching logic to it. */
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