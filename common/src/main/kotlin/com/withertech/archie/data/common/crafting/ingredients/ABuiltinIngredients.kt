package com.withertech.archie.data.common.crafting.ingredients

object ABuiltinIngredients
{
	fun init()
	{
		IACustomIngredientSerializer.register(AAllIngredient.Serializer)
		IACustomIngredientSerializer.register(AAnyIngredient.Serializer)

		IACustomIngredientSerializer.register(AComponentsIngredient.Serializer)
		IACustomIngredientSerializer.register(ACustomDataIngredient.Serializer)
	}
}