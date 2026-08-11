package net.kernelpanicsoft.archie.data.common.conditions

import com.mojang.serialization.*
import dev.nyon.klf.MOD_BUS
import net.minecraft.core.Holder
import net.minecraft.core.RegistryAccess
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.common.conditions.ICondition
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import java.util.stream.Stream
import net.kernelpanicsoft.archie.data.common.conditions.IACondition as ArchieCondition

/**
 * NeoForge implementation of [AConditionsPlatform], adapting Archie's platform-neutral
 * [ArchieCondition] onto NeoForge's `ICondition` API.
 *
 * Every [ArchieCondition] is wrapped as a [NeoForgeCondition] to cross into NeoForge's condition
 * system, and unwrapped again via [ICondition.archie] when Archie code needs the original back.
 */
actual object AConditionsPlatform
{
	/** Registers [identifier] as a NeoForge condition serializer, backed by [codec] via the [NeoForgeConditionCodec] wrapper. */
	actual fun register(identifier: ResourceLocation, codec: MapCodec<out ArchieCondition>)
	{
		val registry = DeferredRegister.create(NeoForgeRegistries.CONDITION_SERIALIZERS, identifier.namespace)
		registry.register(identifier.path) { _ ->
			codec.neoforge
		}
		registry.register(MOD_BUS)
	}

	/** Codec for [ArchieCondition] backed by `ICondition.CODEC`, round-tripping through [neoforge]/[archie]. */
	actual fun codec(): Codec<ArchieCondition>
	{
		return ICondition.CODEC.xmap({
			it.archie
		}, {
			it.neoforge
		})
	}
}

/** Unwraps a NeoForge [ICondition] back to its originating [ArchieCondition]. Throws if it wasn't created via [neoforge]. */
val ICondition.archie: ArchieCondition
	get() = ((this as? NeoForgeCondition) ?: throw AssertionError()).condition

/** Wraps this condition as a NeoForge [ICondition]. Public so archie-datagen can attach conditions to generated recipes. */
val ArchieCondition.neoforge: NeoForgeCondition
	get() = NeoForgeCondition(this)

/** Wraps this codec as a NeoForge condition-serializer codec. */
val MapCodec<out ArchieCondition>.neoforge: MapCodec<NeoForgeCondition>
	get() = NeoForgeConditionCodec(this)

/** Adapts an [ArchieCondition] to NeoForge's [ICondition] interface, delegating [test] to it and [codec] to the registered serializer. */
class NeoForgeCondition(
	val condition: ArchieCondition
) : ICondition
{
	override fun test(iContext: ICondition.IContext): Boolean
	{
		return condition.test(object : ArchieCondition.IContext
		{
			override fun <T> getAllTags(registry: ResourceKey<out Registry<T>>): Map<ResourceLocation, Collection<Holder<T>>>
			{
				return iContext.getAllTags(registry)
			}

			override fun <T> getRegistry(registry: ResourceKey<out Registry<T>>): Registry<T>
			{
				return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).registryOrThrow(registry)
			}
		})
	}

	override fun codec(): MapCodec<out ICondition>
	{
		return NeoForgeRegistries.CONDITION_SERIALIZERS[condition.identifier]!!
	}
}

/** Adapts a [MapCodec] of [ArchieCondition] to one producing/consuming [NeoForgeCondition] wrappers. */
class NeoForgeConditionCodec(
	private val codec: MapCodec<out ArchieCondition>
) : MapCodec<NeoForgeCondition>()
{
	override fun <T> encode(
		input: NeoForgeCondition,
		ops: DynamicOps<T>,
		prefix: RecordBuilder<T>
	): RecordBuilder<T>
	{
		@Suppress("UNCHECKED_CAST")
		return (codec as MapCodec<ArchieCondition>).encode(input.condition, ops, prefix)
	}

	override fun <T> keys(ops: DynamicOps<T>): Stream<T>
	{
		return codec.keys(ops)
	}

	override fun <T> decode(ops: DynamicOps<T>, input: MapLike<T>): DataResult<NeoForgeCondition>
	{
		return codec.decode(ops, input).map { result ->
			result.neoforge
		}
	}
}
