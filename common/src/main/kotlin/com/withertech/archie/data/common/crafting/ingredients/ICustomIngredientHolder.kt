package com.withertech.archie.data.common.crafting.ingredients

interface ICustomIngredientHolder<T : ICustomIngredient>
{
	val custom: T
}