package com.withertech.archie.data.common.conditions

import com.google.common.base.Joiner
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.withertech.archie.Archie
import net.minecraft.resources.ResourceLocation

data object TrueCondition :
	ICondition
{
	val CODEC: MapCodec<TrueCondition> = MapCodec.unit(TrueCondition).stable()
	val ID: ResourceLocation = ResourceLocation(Archie.MOD_ID, "true")
	override fun test(context: ICondition.IContext): Boolean
	{
		return true
	}

	override val codec: MapCodec<out ICondition> = CODEC
	override val identifier: ResourceLocation = ID

	override fun toString(): String
	{
		return "true"
	}
}