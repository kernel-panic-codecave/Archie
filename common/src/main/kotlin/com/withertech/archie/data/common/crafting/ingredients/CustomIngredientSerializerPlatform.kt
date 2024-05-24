package com.withertech.archie.data.common.crafting.ingredients

import dev.architectury.injectables.annotations.ExpectPlatform
import net.minecraft.resources.ResourceLocation

internal object CustomIngredientSerializerPlatform
{
	@ExpectPlatform
	@JvmStatic
	fun register(serializer: ICustomIngredientSerializer<*>)
	{
		throw AssertionError()
	}
}