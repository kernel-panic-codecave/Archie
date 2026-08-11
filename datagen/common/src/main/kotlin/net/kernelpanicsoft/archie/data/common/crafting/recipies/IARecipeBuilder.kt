package net.kernelpanicsoft.archie.data.common.crafting.recipies

import net.kernelpanicsoft.archie.data.common.conditions.IACondition
import net.kernelpanicsoft.archie.data.common.conditions.gen.AConditionBuilder
import net.kernelpanicsoft.archie.data.common.conditions.gen.withCondition
import net.minecraft.data.recipes.RecipeBuilder
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.resources.ResourceLocation

/** [RecipeBuilder] extension adding a condition-aware [save] overload. */
interface IARecipeBuilder : RecipeBuilder
{
	/** Saves this recipe to [recipeOutput] under [id] (or the default id if `null`), gated by the [IACondition] built from [condition]. */
	fun save(recipeOutput: RecipeOutput, id: ResourceLocation? = null, condition: AConditionBuilder.() -> IACondition)
	{
		if (id != null)
			save(recipeOutput.withCondition(condition), id)
		else
			save(recipeOutput.withCondition(condition))
	}
}