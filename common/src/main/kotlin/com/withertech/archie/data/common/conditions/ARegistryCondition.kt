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

data class ARegistryCondition(private val registry: @Serializable(with = ResourceLocationSerializer::class) ResourceLocation, private val entries: List<@Serializable(with = ResourceLocationSerializer::class) ResourceLocation>) :
	IACondition
{
	constructor(registry: ResourceLocation, vararg entries: ResourceLocation) : this(registry, entries.toList())

	override fun test(context: IACondition.IContext): Boolean
	{
		val registryRef: ResourceKey<out Registry<Any>> = ResourceKey.createRegistryKey(registry)
		val registry: Registry<Any> = context.getRegistry(registryRef)
		return registry.keySet().map { entries.contains(it) }.reduce { a, b -> a && b }
	}

	@Transient
	override val codec: MapCodec<out IACondition> = CODEC
	@Transient
	override val identifier: ResourceLocation = ID

	override fun toString(): String
	{
		return "registry(\"$registry\", ${Joiner.on(", ").join(entries.map { "\"$it\"" })})"
	}

	companion object
	{
		val CODEC: MapCodec<ARegistryCondition> =
			RecordCodecBuilder.mapCodec { builder: RecordCodecBuilder.Instance<ARegistryCondition> ->
				builder.group(
					ResourceLocation.CODEC.optionalFieldOf("registry", ResourceLocation.parse("item")).forGetter(ARegistryCondition::registry),
					ResourceLocation.CODEC.listOf().fieldOf("entries").forGetter(ARegistryCondition::entries)
				).apply(builder, ::ARegistryCondition)
			}
		val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(Archie.MOD_ID, "registry")
	}

}
