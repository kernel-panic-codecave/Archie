package com.withertech.archie.data.common.conditions

import com.mojang.serialization.MapCodec
import net.minecraft.resources.ResourceLocation

object ABuiltinConditions
{
	fun init()
	{
		register(AModLoadedCondition.ID, AModLoadedCondition.CODEC)
		register(ARegistryCondition.ID, ARegistryCondition.CODEC)
		register(APlatformCondition.ID, APlatformCondition.CODEC)

		register(AAndCondition.ID, AAndCondition.CODEC)
		register(AOrCondition.ID, AOrCondition.CODEC)
		register(AXorCondition.ID, AXorCondition.CODEC)
		register(ANotCondition.ID, ANotCondition.CODEC)
		register(AEqualsCondition.ID, AEqualsCondition.CODEC)

		register(ATrueCondition.ID, ATrueCondition.CODEC)
		register(AFalseCondition.ID, AFalseCondition.CODEC)
	}

	private inline fun <reified T : IACondition> register(identifier: ResourceLocation, codec: MapCodec<T>)
	{
		IACondition.register(identifier, codec)
	}
}