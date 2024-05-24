package com.withertech.archie.data.common.conditions

import com.google.common.base.Joiner
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.withertech.archie.Archie
import net.minecraft.resources.ResourceLocation

data class XorCondition(override val children: List<ICondition>) :
	GroupCondition()
{
	constructor(vararg values: ICondition) : this(values.toList())

	override fun reducer(a: Boolean, b: Boolean): Boolean = a xor b

	override val codec: MapCodec<out ICondition> = CODEC
	override val identifier: ResourceLocation = ID

	override fun toString(): String
	{
		return "(${Joiner.on(" ^^ ").join(children)})"
	}

	companion object
	{
		val CODEC: MapCodec<XorCondition> =
			RecordCodecBuilder.mapCodec { builder: RecordCodecBuilder.Instance<XorCondition> ->
				builder.group(
					ICondition.CODEC.listOf().fieldOf("children").forGetter(XorCondition::children),
				).apply(builder, ::XorCondition)
			}
		val ID: ResourceLocation = ResourceLocation(Archie.MOD_ID, "xor")
	}
}