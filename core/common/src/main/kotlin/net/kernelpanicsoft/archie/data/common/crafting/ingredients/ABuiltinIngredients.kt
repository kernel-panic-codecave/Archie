package net.kernelpanicsoft.archie.data.common.crafting.ingredients

/** Registers Archie's built-in [IACustomIngredient] serializers ([AAllIngredient], [AAnyIngredient], [AComponentsIngredient], [ACustomDataIngredient]). */
object ABuiltinIngredients
{
	/** Registers every built-in ingredient serializer; called once during [net.kernelpanicsoft.archie.Archie.init]. */
	fun init()
	{
		IACustomIngredientSerializer.register(AAllIngredient.Serializer)
		IACustomIngredientSerializer.register(AAnyIngredient.Serializer)

		IACustomIngredientSerializer.register(AComponentsIngredient.Serializer)
		IACustomIngredientSerializer.register(ACustomDataIngredient.Serializer)
	}
}