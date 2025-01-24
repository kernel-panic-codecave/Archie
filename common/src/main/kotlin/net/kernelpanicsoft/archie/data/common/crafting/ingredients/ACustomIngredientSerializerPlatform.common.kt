package net.kernelpanicsoft.archie.data.common.crafting.ingredients


internal expect object ACustomIngredientSerializerPlatform
{
	fun register(serializer: IACustomIngredientSerializer<*>)
}