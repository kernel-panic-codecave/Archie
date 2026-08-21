package net.kernelpanicsoft.archie.registries

import net.kernelpanicsoft.archie.util.div
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.resources.ResourceLocation

object CustomModelRegistry
{
	fun registerItem(id: ModelResourceLocation)
	{
		models["item" / id.id] = id
	}
	fun registerBlock(id: ModelResourceLocation)
	{
		models["block" / id.id] = id
	}
	@JvmField
	internal val models: MutableMap<ResourceLocation, ModelResourceLocation> = mutableMapOf()
}