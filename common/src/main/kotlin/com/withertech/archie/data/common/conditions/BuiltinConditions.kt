package com.withertech.archie.data.common.conditions

import com.mojang.serialization.MapCodec
import net.minecraft.resources.ResourceLocation

object BuiltinConditions
{
	fun init()
	{
		register(ModLoadedCondition.ID, ModLoadedCondition.CODEC)
		register(RegistryCondition.ID, RegistryCondition.CODEC)
		register(PlatformCondition.ID, PlatformCondition.CODEC)

		register(AndCondition.ID, AndCondition.CODEC)
		register(OrCondition.ID, OrCondition.CODEC)
		register(XorCondition.ID, XorCondition.CODEC)
		register(NotCondition.ID, NotCondition.CODEC)
		register(EqualsCondition.ID, EqualsCondition.CODEC)

		register(TrueCondition.ID, TrueCondition.CODEC)
		register(FalseCondition.ID, FalseCondition.CODEC)
	}

	private inline fun <reified T : ICondition> register(identifier: ResourceLocation, codec: MapCodec<T>)
	{
		ICondition.register(identifier, codec)
	}
}