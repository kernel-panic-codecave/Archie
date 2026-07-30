package net.kernelpanicsoft.archie.data.common.conditions

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import net.kernelpanicsoft.archie.data.common.crafting.ARecipeProvider
import net.minecraft.core.HolderLookup
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.RecipeProvider
import net.minecraft.resources.ResourceLocation
import java.util.concurrent.CompletableFuture

/**
 * Cross-loader hooks that plug [IACondition] into each loader's native datapack condition
 * system, since Fabric and NeoForge each have their own recipe/tag condition machinery.
 */
expect object AConditionsPlatform
{
	/** Registers condition type keyed by [identifier], decodable with [codec]; backs [IACondition.register]. */
	fun register(identifier: ResourceLocation, codec: MapCodec<out IACondition>)

	/** Attaches [condition] to the next recipe written to [output] via the loader's native mechanism. */
	fun withCondition(output: RecipeOutput, condition: IACondition): RecipeOutput

	/** The dispatch codec decoding any registered [IACondition]; backs [IACondition.CODEC]. */
	fun codec(): Codec<IACondition>

	/** Wraps [child] in a loader-specific [RecipeProvider] so [withCondition] can attach conditions on Fabric; `null` where not needed. */
	fun fabricRecipeProvider(child: ARecipeProvider, registries: CompletableFuture<HolderLookup.Provider>): RecipeProvider?
}