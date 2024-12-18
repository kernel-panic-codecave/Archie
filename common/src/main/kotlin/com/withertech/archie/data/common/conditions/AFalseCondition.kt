package com.withertech.archie.data.common.conditions

import com.mojang.serialization.MapCodec
import com.withertech.archie.Archie
import net.minecraft.resources.ResourceLocation

data object AFalseCondition :
	IACondition
{
	val CODEC: MapCodec<AFalseCondition> = MapCodec.unit(AFalseCondition).stable()
	val ID: ResourceLocation = Archie["false"]
	override fun test(context: IACondition.IContext): Boolean
	{
		return false
	}

	override val codec: MapCodec<out IACondition> = CODEC
	override val identifier: ResourceLocation = ID

	override fun toString(): String
	{
		return "false"
	}
}