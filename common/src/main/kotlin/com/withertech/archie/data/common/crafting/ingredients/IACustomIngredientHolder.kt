package com.withertech.archie.data.common.crafting.ingredients

interface IACustomIngredientHolder<T : IACustomIngredient>
{
	val custom: T
}