package net.kernelpanicsoft.archie.data.common.crafting.ingredients

interface IACustomIngredientHolder<T : IACustomIngredient>
{
	val custom: T
}