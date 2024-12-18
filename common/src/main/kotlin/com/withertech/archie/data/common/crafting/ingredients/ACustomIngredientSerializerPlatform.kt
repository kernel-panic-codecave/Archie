package com.withertech.archie.data.common.crafting.ingredients


internal expect object ACustomIngredientSerializerPlatform
{
	fun register(serializer: IACustomIngredientSerializer<*>)
}