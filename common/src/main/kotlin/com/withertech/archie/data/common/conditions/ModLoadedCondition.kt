package com.withertech.archie.data.common.conditions

import com.google.common.base.Joiner
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.withertech.archie.Archie
import dev.architectury.platform.Platform
import net.minecraft.resources.ResourceLocation

data class ModLoadedCondition(val mods: List<String>) : ICondition
{
	constructor(vararg mods: String) : this(mods.toList())
	override fun test(context: ICondition.IContext): Boolean
	{
		return mods.map(Platform::isModLoaded).reduce { a, b -> a && b }
	}

	override val codec: MapCodec<out ICondition> = CODEC
	override val identifier: ResourceLocation = ID

	override fun toString(): String
	{
		return "mod_loaded(${Joiner.on(", ").join(mods.map { "\"$it\"" })})"
	}

	companion object
	{
		val CODEC: MapCodec<ModLoadedCondition> =
			RecordCodecBuilder.mapCodec { builder: RecordCodecBuilder.Instance<ModLoadedCondition> ->
				builder
					.group(
						Codec.STRING.listOf().fieldOf("mods").forGetter(ModLoadedCondition::mods)
					)
					.apply(
						builder
					) { mods: List<String> ->
						ModLoadedCondition(
							mods
						)
					}
			}
		val ID: ResourceLocation = ResourceLocation(Archie.MOD_ID, "mod_loaded")
	}
}
