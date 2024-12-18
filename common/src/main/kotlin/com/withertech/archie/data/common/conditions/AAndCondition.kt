package com.withertech.archie.data.common.conditions

import com.google.common.base.Joiner
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.withertech.archie.Archie
import net.minecraft.resources.ResourceLocation

data class AAndCondition(override val children: List<IACondition>) :
	AGroupCondition()
{
	constructor(vararg values: IACondition) : this(values.toList())

	override fun reducer(a: Boolean, b: Boolean): Boolean = a and b

	override val codec: MapCodec<out IACondition> = CODEC
	override val identifier: ResourceLocation = ID

	override fun toString(): String
	{
		return "(${Joiner.on(" && ").join(children)})"
	}

	companion object
	{
		val CODEC: MapCodec<AAndCondition> =
			RecordCodecBuilder.mapCodec { builder: RecordCodecBuilder.Instance<AAndCondition> ->
				builder.group(
					IACondition.CODEC.listOf().fieldOf("children").forGetter(AAndCondition::children)
				).apply(builder, ::AAndCondition)
			}
		val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(Archie.MOD_ID, "and")
	}
}