package com.withertech.archie.data.common.conditions

import com.google.common.base.Joiner
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.withertech.archie.Archie
import dev.architectury.platform.Platform
import net.minecraft.resources.ResourceLocation

data class AModLoadedCondition(val mods: List<String>) : IACondition
{
	constructor(vararg mods: String) : this(mods.toList())
	override fun test(context: IACondition.IContext): Boolean
	{
		return mods.map(Platform::isModLoaded).reduce { a, b -> a && b }
	}

	override val codec: MapCodec<out IACondition> = CODEC
	override val identifier: ResourceLocation = ID

	override fun toString(): String
	{
		return "mod_loaded(${Joiner.on(", ").join(mods.map { "\"$it\"" })})"
	}

	companion object
	{
		val CODEC: MapCodec<AModLoadedCondition> =
			RecordCodecBuilder.mapCodec { builder: RecordCodecBuilder.Instance<AModLoadedCondition> ->
				builder
					.group(
						Codec.STRING.listOf().fieldOf("mods").forGetter(AModLoadedCondition::mods)
					)
					.apply(
						builder
					) { mods: List<String> ->
						AModLoadedCondition(
							mods
						)
					}
			}
		val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(Archie.MOD_ID, "mod_loaded")
	}
}
