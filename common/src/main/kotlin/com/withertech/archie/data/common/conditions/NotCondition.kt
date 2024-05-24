package com.withertech.archie.data.common.conditions

import com.google.common.base.Joiner
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.withertech.archie.Archie
import net.minecraft.resources.ResourceLocation

data class NotCondition(val child: ICondition) :
	ICondition
{
	override fun test(context: ICondition.IContext): Boolean
	{
		return !child.test(context)
	}

	override val codec: MapCodec<out ICondition> = CODEC
	override val identifier: ResourceLocation = ID

	override fun toString(): String
	{
		return "!$child"
	}
	companion object
	{
		val CODEC: MapCodec<NotCondition> =
			RecordCodecBuilder.mapCodec { builder: RecordCodecBuilder.Instance<NotCondition> ->
				builder.group(
					ICondition.CODEC.fieldOf("child").forGetter(NotCondition::child)
				).apply(builder, ::NotCondition)
			}
		val ID: ResourceLocation = ResourceLocation(Archie.MOD_ID, "not")
	}
}