package net.kernelpanicsoft.archie.data.common.crafting.ingredients


internal expect object ACustomIngredientSerializerPlatform
{
	fun <T : IACustomIngredient> register(serializer: IACustomIngredientSerializer<T>)
}