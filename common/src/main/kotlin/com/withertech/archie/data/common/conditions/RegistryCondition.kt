package com.withertech.archie.data.common.conditions

import com.google.common.base.Joiner
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.withertech.archie.Archie
import com.withertech.archie.serialization.ResourceLocationSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation

data class RegistryCondition(private val registry: @Serializable(with = ResourceLocationSerializer::class) ResourceLocation, private val entries: List<@Serializable(with = ResourceLocationSerializer::class) ResourceLocation>) :
	ICondition
{
	constructor(registry: ResourceLocation, vararg entries: ResourceLocation) : this(registry, entries.toList())

	override fun test(context: ICondition.IContext): Boolean
	{
		val registryRef: ResourceKey<out Registry<Any>> = ResourceKey.createRegistryKey(registry)
		val registry: Registry<Any> = context.getRegistry(registryRef)
		return registry.keySet().map { entries.contains(it) }.reduce { a, b -> a && b }
	}

	@Transient
	override val codec: MapCodec<out ICondition> = CODEC
	@Transient
	override val identifier: ResourceLocation = ID

	override fun toString(): String
	{
		return "registry(\"$registry\", ${Joiner.on(", ").join(entries.map { "\"$it\"" })})"
	}

	companion object
	{
		val CODEC: MapCodec<RegistryCondition> =
			RecordCodecBuilder.mapCodec { builder: RecordCodecBuilder.Instance<RegistryCondition> ->
				builder.group(
					ResourceLocation.CODEC.optionalFieldOf("registry", ResourceLocation("item")).forGetter(RegistryCondition::registry),
					ResourceLocation.CODEC.listOf().fieldOf("entries").forGetter(RegistryCondition::entries)
				).apply(builder, ::RegistryCondition)
			}
		val ID: ResourceLocation = ResourceLocation(Archie.MOD_ID, "registry")
	}

}
