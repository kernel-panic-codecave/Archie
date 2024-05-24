package com.withertech.archie.data.common.conditions.fabric

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.withertech.archie.data.common.conditions.ICondition
import com.withertech.archie.data.common.crafting.ArchieRecipeProvider
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions
import net.fabricmc.fabric.impl.datagen.FabricDataGenHelper
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
import java.util.concurrent.CompletableFuture

object ConditionsPlatformImpl
{
	private val registry: MutableMap<ResourceLocation, ResourceConditionType<FabricCondition>> = mutableMapOf()

	@JvmStatic
	fun register(identifier: ResourceLocation, codec: MapCodec<out ICondition>)
	{
		@Suppress("UNCHECKED_CAST")
		registry[identifier] = ResourceConditionType.create(identifier, (codec as MapCodec<ICondition>).xmap({
			it.fabric
		}, {
			it.condition
		}))
		ResourceConditions.register(registry[identifier])
	}

	@JvmStatic
	fun withCondition(output: RecipeOutput, condition: ICondition): RecipeOutput
	{
		return object : RecipeOutput
		{
			@Suppress("UnstableApiUsage")
			override fun accept(identifier: ResourceLocation, recipe: Recipe<*>, advancementEntry: AdvancementHolder?)
			{
				FabricDataGenHelper.addConditions(recipe, arrayOf(condition.fabric))
				output.accept(identifier, recipe, advancementEntry)
			}

			override fun advancement(): Advancement.Builder
			{
				return output.advancement()
			}
		}
	}

	@JvmStatic
	fun codec(): Codec<out ICondition>
	{
		return ResourceCondition.CODEC.xmap(
			{ resourceCondition ->
				resourceCondition.archie
			}, { iCondition ->
				iCondition.fabric
			}
		)
	}

	@JvmStatic
	fun fabricRecipeProvider(
		child: ArchieRecipeProvider,
		registries: CompletableFuture<HolderLookup.Provider>
	): RecipeProvider
	{
		return object : FabricRecipeProvider(child.output as FabricDataOutput, registries)
		{
			override fun buildRecipes(exporter: RecipeOutput)
			{
				child.buildRecipes(exporter)
			}
		}
	}

	val ICondition.fabric
		get() = FabricCondition(this)

	val ResourceCondition.archie
		get() = ((this as? FabricCondition) ?: throw AssertionError()).condition

	class FabricCondition(
		val condition: ICondition
	) : ResourceCondition
	{
		override fun getType(): ResourceConditionType<*>
		{
			return registry[condition.identifier]!!
		}

		override fun test(registryLookup: HolderLookup.Provider?): Boolean
		{
			return registryLookup?.let {
				condition.test(ConditionContext(it))
			} ?: false
		}
	}

	class ConditionContext(private val registryLookup: HolderLookup.Provider) :
		ICondition.IContext
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
}