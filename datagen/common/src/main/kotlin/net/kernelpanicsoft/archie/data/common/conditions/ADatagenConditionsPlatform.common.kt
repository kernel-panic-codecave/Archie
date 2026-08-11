package net.kernelpanicsoft.archie.data.common.conditions

import net.kernelpanicsoft.archie.data.common.crafting.ARecipeProvider
import net.minecraft.core.HolderLookup
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.RecipeProvider
import java.util.concurrent.CompletableFuture

/**
 * Datagen-only half of [AConditionsPlatform] - attaching conditions to a *generated* recipe.
 * Split out from [AConditionsPlatform] itself since neither member has any runtime presence.
 */
expect object ADatagenConditionsPlatform
{
	/** Attaches [condition] to the next recipe written to [output] via the loader's native mechanism. */
	fun withCondition(output: RecipeOutput, condition: IACondition): RecipeOutput

	/** Wraps [child] in a loader-specific [RecipeProvider] so [withCondition] can attach conditions on Fabric; `null` where not needed. */
	fun fabricRecipeProvider(child: ARecipeProvider, registries: CompletableFuture<HolderLookup.Provider>): RecipeProvider?
}
