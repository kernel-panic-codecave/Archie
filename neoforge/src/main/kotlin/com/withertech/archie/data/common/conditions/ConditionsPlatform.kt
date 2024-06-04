package com.withertech.archie.data.common.conditions

import com.mojang.serialization.*
import com.withertech.archie.data.common.crafting.ArchieRecipeProvider
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
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import java.util.concurrent.CompletableFuture
import java.util.stream.Stream
import com.withertech.archie.data.common.conditions.ICondition as ArchieCondition


actual object ConditionsPlatform
{
	actual fun register(identifier: ResourceLocation, codec: MapCodec<out ArchieCondition>)
	{
		val registry = DeferredRegister.create(NeoForgeRegistries.CONDITION_SERIALIZERS, identifier.namespace)
		registry.register(identifier.path) { _ ->
			codec.neoforge
		}
		registry.register(MOD_BUS)
	}

	actual fun withCondition(output: RecipeOutput, condition: ArchieCondition): RecipeOutput
	{
		return NeoForgeConditionalRecipeOutput(output, condition.neoforge)
	}

	actual fun codec(): Codec<ArchieCondition>
	{
		return ICondition.CODEC.xmap({
			it.archie
		}, {
			it.neoforge
		})
	}

	actual fun fabricRecipeProvider(child: ArchieRecipeProvider, registries: CompletableFuture<HolderLookup.Provider>): RecipeProvider? = null

	val ICondition.archie
		get() = ((this as? NeoForgeCondition) ?: throw AssertionError()).condition

	val ArchieCondition.neoforge
		get() = NeoForgeCondition(this)

	val MapCodec<out ArchieCondition>.neoforge
		get() = NeoForgeConditionCodec(this)

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

	class NeoForgeConditionalRecipeOutput(private val inner: RecipeOutput, private val condition: ICondition) :
		RecipeOutput
	{
		override fun advancement(): Advancement.Builder
		{
			return inner.advancement()
		}

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