package com.withertech.archie.data.common.crafting.ingredients


internal expect object CustomIngredientSerializerPlatform
{
	fun register(serializer: ICustomIngredientSerializer<*>)
}