package net.kernelpanicsoft.archie.data.common.conditions

import com.mojang.serialization.MapCodec
import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.util.rem
import net.minecraft.resources.ResourceLocation

/** Condition that never holds. */
data object AFalseCondition :
	IACondition
{
	val CODEC: MapCodec<AFalseCondition> = MapCodec.unit(AFalseCondition).stable()
	val ID: ResourceLocation = Archie % "false"
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