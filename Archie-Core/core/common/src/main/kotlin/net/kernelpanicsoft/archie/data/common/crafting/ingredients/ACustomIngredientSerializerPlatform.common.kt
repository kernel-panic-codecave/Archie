package net.kernelpanicsoft.archie.data.common.crafting.ingredients


/** Cross-loader hook registering an [IACustomIngredientSerializer]; backs [IACustomIngredientSerializer.register]. */
internal expect object ACustomIngredientSerializerPlatform
{
	fun <T : IACustomIngredient> register(serializer: IACustomIngredientSerializer<T>)
}