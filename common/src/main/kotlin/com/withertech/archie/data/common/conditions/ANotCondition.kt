package com.withertech.archie.data.common.conditions

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.withertech.archie.Archie
import net.minecraft.resources.ResourceLocation

data class ANotCondition(val child: IACondition) :
	IACondition
{
	override fun test(context: IACondition.IContext): Boolean
	{
		return !child.test(context)
	}

	override val codec: MapCodec<out IACondition> = CODEC
	override val identifier: ResourceLocation = ID

	override fun toString(): String
	{
		return "!$child"
	}
	companion object
	{
		val CODEC: MapCodec<ANotCondition> =
			RecordCodecBuilder.mapCodec { builder: RecordCodecBuilder.Instance<ANotCondition> ->
				builder.group(
					IACondition.CODEC.fieldOf("child").forGetter(ANotCondition::child)
				).apply(builder, ::ANotCondition)
			}
		val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(Archie.MOD_ID, "not")
	}
}