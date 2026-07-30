package net.kernelpanicsoft.archie.data.common.conditions

import com.mojang.serialization.*
import dev.nyon.klf.MOD_BUS
import net.kernelpanicsoft.archie.data.common.crafting.ARecipeProvider
import net.minecraft.advancements.Advancement
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.core.Holder
import net.minecraft.core.HolderLookup
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.RecipeProvider
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.crafting.Recipe
import net.neoforged.neoforge.common.conditions.ICondition
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import java.util.concurrent.CompletableFuture
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

	/** Wraps [output] so every entry accepted through it also carries [condition]. */
	actual fun withCondition(output: RecipeOutput, condition: ArchieCondition): RecipeOutput
	{
		return NeoForgeConditionalRecipeOutput(output, condition.neoforge)
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

	/** Fabric-only concern; always `null` on NeoForge. */
	actual fun fabricRecipeProvider(child: ARecipeProvider, registries: CompletableFuture<HolderLookup.Provider>): RecipeProvider? = null

	/** Unwraps a NeoForge [ICondition] back to its originating [ArchieCondition]. Throws if it wasn't created via [neoforge]. */
	val ICondition.archie
		get() = ((this as? NeoForgeCondition) ?: throw AssertionError()).condition

	/** Wraps this condition as a NeoForge [ICondition]. */
	val ArchieCondition.neoforge
		get() = NeoForgeCondition(this)

	/** Wraps this codec as a NeoForge condition-serializer codec. */
	val MapCodec<out ArchieCondition>.neoforge
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

	/** [RecipeOutput] wrapper that always attaches [condition] to whatever [inner] accepts. */
	class NeoForgeConditionalRecipeOutput(private val inner: RecipeOutput, private val condition: ICondition) :
		RecipeOutput
	{
		override fun advancement(): Advancement.Builder
		{
			return inner.advancement()
		}

		/** Forwards to [inner], attaching [condition]; any [iConditions] passed by the caller are not currently applied. */
		override fun accept(
			id: ResourceLocation,
			recipe: Recipe<*>,
			adv: AdvancementHolder?,
			vararg iConditions: ICondition
		)
		{
			inner.accept(id, recipe, adv, this.condition)
		}
	}
}