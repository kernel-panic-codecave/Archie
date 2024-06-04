package com.withertech.archie.data.common.conditions

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.withertech.archie.Archie
import com.withertech.archie.ArchiePlatform
import kotlinx.serialization.Transient
import net.minecraft.resources.ResourceLocation

data class PlatformCondition(val platform: String) : ICondition
{
	override fun test(context: ICondition.IContext): Boolean
	{
		return ArchiePlatform.platform == platform
	}

	@Transient
	override val codec: MapCodec<out ICondition> = CODEC
	@Transient
	override val identifier: ResourceLocation = ID

	override fun toString(): String
	{
		return "platform(\"$platform\")"
	}

	companion object
	{
		val CODEC: MapCodec<PlatformCondition> =
			RecordCodecBuilder.mapCodec { builder: RecordCodecBuilder.Instance<PlatformCondition> ->
				builder
					.group(
						Codec.STRING.fieldOf("platform").forGetter(PlatformCondition::platform)
					)
					.apply(
						builder
					) { platform: String ->
						PlatformCondition(
							platform
						)
					}
			}
		val ID: ResourceLocation = ResourceLocation(Archie.MOD_ID, "platform")
	}
}
