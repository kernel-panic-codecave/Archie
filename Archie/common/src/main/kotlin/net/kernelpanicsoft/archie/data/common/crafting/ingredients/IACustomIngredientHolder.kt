package net.kernelpanicsoft.archie.data.common.crafting.ingredients

/**
 * Implemented by the vanilla `Ingredient` produced from [IACustomIngredient.vanilla], exposing
 * the wrapped [custom] ingredient so it can be recovered from a vanilla `Ingredient` reference.
 */
interface IACustomIngredientHolder<T : IACustomIngredient>
{
	/** The custom ingredient this vanilla `Ingredient` was converted from. */
	val custom: T
}