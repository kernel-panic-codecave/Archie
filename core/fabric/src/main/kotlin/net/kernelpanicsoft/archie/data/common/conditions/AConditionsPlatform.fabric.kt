package net.kernelpanicsoft.archie.data.common.conditions

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions
import net.minecraft.core.Holder
import net.minecraft.core.HolderLookup
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation

/**
 * Fabric implementation of [AConditionsPlatform], adapting Archie's platform-neutral [IACondition]
 * onto Fabric's `ResourceCondition` API (`fabric-resource-conditions-api-v1`).
 *
 * Every [IACondition] is wrapped as a [FabricCondition] to cross into Fabric's condition system,
 * and unwrapped again via [ResourceCondition.archie] when Archie code needs the original back.
 */
actual object AConditionsPlatform
{
	/** Fabric condition types registered via [register], keyed by their [IACondition.identifier]. Public so archie-datagen's `ADatagenConditionsPlatform` can wrap conditions too. */
	val registry: MutableMap<ResourceLocation, ResourceConditionType<FabricCondition>> = mutableMapOf()

	/** Registers [identifier] as a Fabric [ResourceConditionType], backed by [codec] via the [FabricCondition] wrapper. */
	actual fun register(identifier: ResourceLocation, codec: MapCodec<out IACondition>)
	{
		@Suppress("UNCHECKED_CAST")
		registry[identifier] = ResourceConditionType.create(identifier, (codec as MapCodec<IACondition>).xmap({
			it.fabric
		}, {
			it.condition
		}))
		ResourceConditions.register(registry[identifier])
	}

	/** Codec for [IACondition] backed by [ResourceCondition.CODEC], round-tripping through [fabric]/[archie]. */
	actual fun codec(): Codec<IACondition>
	{
		return ResourceCondition.CODEC.xmap(
			{ resourceCondition ->
				resourceCondition.archie
			}, { iCondition ->
				iCondition.fabric
			}
		)
	}
}

/** Wraps this condition as a Fabric [ResourceCondition]. Public so archie-datagen can attach conditions to generated recipes. */
val IACondition.fabric: FabricCondition
	get() = FabricCondition(this)

/** Unwraps a Fabric [ResourceCondition] back to its originating [IACondition]. Throws if it wasn't created via [fabric]. */
val ResourceCondition.archie: IACondition
	get() = ((this as? FabricCondition) ?: throw AssertionError()).condition

/** Adapts an [IACondition] to Fabric's [ResourceCondition] interface, delegating [getType] and [test] to it. */
class FabricCondition(
	val condition: IACondition
) : ResourceCondition
{
	override fun getType(): ResourceConditionType<*>
	{
		return AConditionsPlatform.registry[condition.identifier]!!
	}

	override fun test(registryLookup: HolderLookup.Provider?): Boolean
	{
		return registryLookup?.let {
			condition.test(ConditionContext(it))
		} ?: false
	}
}

/** [IACondition.IContext] backed directly by a Fabric registry lookup, used when Fabric evaluates a condition. */
class ConditionContext(private val registryLookup: HolderLookup.Provider) :
	IACondition.IContext
{
	override fun <T> getAllTags(registry: ResourceKey<out Registry<T>>): Map<ResourceLocation, Collection<Holder<T>>>
	{
		return registryLookup.lookupOrThrow(registry).listTags().toList()
			.associateBy({ it.key().location }, { it.toList() })
	}

	override fun <T> getRegistry(registry: ResourceKey<out Registry<T>>): Registry<T>
	{
		return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).registryOrThrow(registry)
	}
}
