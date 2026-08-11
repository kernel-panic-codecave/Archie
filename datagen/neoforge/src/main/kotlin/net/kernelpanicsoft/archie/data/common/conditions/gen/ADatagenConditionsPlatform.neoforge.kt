package net.kernelpanicsoft.archie.data.common.conditions.gen

import net.kernelpanicsoft.archie.data.common.conditions.IACondition
import net.kernelpanicsoft.archie.data.common.conditions.neoforge
import net.kernelpanicsoft.archie.data.common.crafting.ARecipeProvider
import net.minecraft.advancements.Advancement
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.core.HolderLookup
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.RecipeProvider
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.crafting.Recipe
import net.neoforged.neoforge.common.conditions.ICondition
import java.util.concurrent.CompletableFuture

/** NeoForge implementation of [ADatagenConditionsPlatform]. */
actual object ADatagenConditionsPlatform
{
	/** Wraps [output] so every entry accepted through it also carries [condition]. */
	actual fun withCondition(output: RecipeOutput, condition: IACondition): RecipeOutput
	{
		return NeoForgeConditionalRecipeOutput(output, condition.neoforge)
	}

	/** Fabric-only concern; always `null` on NeoForge. */
	actual fun fabricRecipeProvider(child: ARecipeProvider, registries: CompletableFuture<HolderLookup.Provider>): RecipeProvider? = null

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
