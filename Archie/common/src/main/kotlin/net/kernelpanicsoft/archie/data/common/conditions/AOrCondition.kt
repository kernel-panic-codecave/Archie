package net.kernelpanicsoft.archie.data.common.conditions

import com.google.common.base.Joiner
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.kernelpanicsoft.archie.Archie
import net.minecraft.resources.ResourceLocation

/** Condition that holds when at least one of [children] holds (logical OR). */
data class AOrCondition(override val children: List<IACondition>) :
	AGroupCondition()
{
	constructor(vararg values: IACondition) : this(values.toList())

	override fun reducer(a: Boolean, b: Boolean): Boolean = a or b

	override val codec: MapCodec<out IACondition> = CODEC
	override val identifier: ResourceLocation = ID

	override fun toString(): String
	{
		return "(${Joiner.on(" || ").join(children)})"
	}

	companion object
	{
		val CODEC: MapCodec<AOrCondition> =
			RecordCodecBuilder.mapCodec { builder: RecordCodecBuilder.Instance<AOrCondition> ->
				builder.group(
					IACondition.CODEC.listOf().fieldOf("children").forGetter(AOrCondition::children),
				).apply(builder, ::AOrCondition)
			}
		val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(Archie.MOD_ID, "or")
	}
}