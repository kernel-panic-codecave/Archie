package com.withertech.archie.data.common.conditions

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.withertech.archie.Archie
import com.withertech.archie.APlatform
import kotlinx.serialization.Transient
import net.minecraft.resources.ResourceLocation

data class APlatformCondition(val platform: String) : IACondition
{
	override fun test(context: IACondition.IContext): Boolean
	{
		return APlatform.platform == platform
	}

	@Transient
	override val codec: MapCodec<out IACondition> = CODEC
	@Transient
	override val identifier: ResourceLocation = ID

	override fun toString(): String
	{
		return "platform(\"$platform\")"
	}

	companion object
	{
		val CODEC: MapCodec<APlatformCondition> =
			RecordCodecBuilder.mapCodec { builder: RecordCodecBuilder.Instance<APlatformCondition> ->
				builder
					.group(
						Codec.STRING.fieldOf("platform").forGetter(APlatformCondition::platform)
					)
					.apply(
						builder
					) { platform: String ->
						APlatformCondition(
							platform
						)
					}
			}
		val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(Archie.MOD_ID, "platform")
	}
}
