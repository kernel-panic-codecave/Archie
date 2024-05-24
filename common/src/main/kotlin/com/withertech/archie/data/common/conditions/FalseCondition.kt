package com.withertech.archie.data.common.conditions

import com.google.common.base.Joiner
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.withertech.archie.Archie
import net.minecraft.resources.ResourceLocation

data object FalseCondition :
	ICondition
{
	val CODEC: MapCodec<FalseCondition> = MapCodec.unit(FalseCondition).stable()
	val ID: ResourceLocation = Archie["false"]
	override fun test(context: ICondition.IContext): Boolean
	{
		return false
	}

	override val codec: MapCodec<out ICondition> = CODEC
	override val identifier: ResourceLocation = ID

	override fun toString(): String
	{
		return "false"
	}
}