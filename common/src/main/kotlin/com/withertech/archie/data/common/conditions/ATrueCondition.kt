package com.withertech.archie.data.common.conditions

import com.mojang.serialization.MapCodec
import com.withertech.archie.Archie
import net.minecraft.resources.ResourceLocation

data object ATrueCondition :
	IACondition
{
	val CODEC: MapCodec<ATrueCondition> = MapCodec.unit(ATrueCondition).stable()
	val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(Archie.MOD_ID, "true")
	override fun test(context: IACondition.IContext): Boolean
	{
		return true
	}

	override val codec: MapCodec<out IACondition> = CODEC
	override val identifier: ResourceLocation = ID

	override fun toString(): String
	{
		return "true"
	}
}