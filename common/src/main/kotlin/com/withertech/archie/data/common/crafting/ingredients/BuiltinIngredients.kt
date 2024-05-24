package com.withertech.archie.data.common.crafting.ingredients

object BuiltinIngredients
{
	fun init()
	{
		ICustomIngredientSerializer.register(AllIngredient.Serializer)
		ICustomIngredientSerializer.register(AnyIngredient.Serializer)

		ICustomIngredientSerializer.register(ComponentsIngredient.Serializer)
		ICustomIngredientSerializer.register(CustomDataIngredient.Serializer)
	}
}