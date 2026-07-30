package net.kernelpanicsoft.archie.data.common.conditions

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.data.common.crafting.ARecipeProvider
import kotlinx.serialization.json.Json
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions
import net.fabricmc.fabric.impl.datagen.FabricDataGenHelper
import net.kernelpanicsoft.archie.serialization.kSerializer
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

/**
 * Fabric implementation of [AConditionsPlatform], adapting Archie's platform-neutral [IACondition]
 * onto Fabric's `ResourceCondition` API (`fabric-resource-conditions-api-v1`).
 *
 * Every [IACondition] is wrapped as a [FabricCondition] to cross into Fabric's condition system,
 * and unwrapped again via [ResourceCondition.archie] when Archie code needs the original back.
 */
actual object AConditionsPlatform
{
	/** Fabric condition types registered via [register], keyed by their [IACondition.identifier]. */
	private val registry: MutableMap<ResourceLocation, ResourceConditionType<FabricCondition>> = mutableMapOf()

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

	/** Attaches [condition] to whatever [output] accepts next, via [FabricDataGenHelper.addConditions]. */
	actual fun withCondition(output: RecipeOutput, condition: IACondition): RecipeOutput
	{
		return object : RecipeOutput
		{
			@Suppress("UnstableApiUsage")
			override fun accept(identifier: ResourceLocation, recipe: Recipe<*>, advancementEntry: AdvancementHolder?)
			{
				FabricDataGenHelper.addConditions(recipe, arrayOf(condition.fabric))
				Archie.LOGGER.info(Json.encodeToString(ResourceCondition.CODEC.kSerializer, condition.fabric))
				output.accept(identifier, recipe, advancementEntry)
			}

			override fun advancement(): Advancement.Builder
			{
				return output.advancement()
			}
		}
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

	/** Wraps [child] in a [FabricRecipeProvider] so its recipes go through Fabric's condition-aware output. */
	actual fun fabricRecipeProvider(
		child: ARecipeProvider,
		registries: CompletableFuture<HolderLookup.Provider>
	): RecipeProvider?
	{
		return object : FabricRecipeProvider(child.output as FabricDataOutput, registries)
		{
			override fun buildRecipes(exporter: RecipeOutput)
			{
				child.buildRecipes(exporter)
			}
		}
	}

	/** Wraps this condition as a Fabric [ResourceCondition]. */
	val IACondition.fabric
		get() = FabricCondition(this)

	/** Unwraps a Fabric [ResourceCondition] back to its originating [IACondition]. Throws if it wasn't created via [fabric]. */
	val ResourceCondition.archie
		get() = ((this as? FabricCondition) ?: throw AssertionError()).condition

	/** Adapts an [IACondition] to Fabric's [ResourceCondition] interface, delegating [getType] and [test] to it. */
	class FabricCondition(
		val condition: IACondition
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
}