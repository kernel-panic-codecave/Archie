package com.withertech.archie.data.common.crafting.recipies

import com.withertech.archie.data.common.conditions.AConditionBuilder
import com.withertech.archie.data.common.conditions.IACondition
import com.withertech.archie.data.common.conditions.withCondition
import net.minecraft.data.recipes.RecipeBuilder
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.resources.ResourceLocation

interface IARecipeBuilder : RecipeBuilder
{
	fun save(recipeOutput: RecipeOutput, id: ResourceLocation? = null, condition: AConditionBuilder.() -> IACondition)
	{
		if (id != null)
			save(recipeOutput.withCondition(condition), id)
		else
			save(recipeOutput.withCondition(condition))
	}
}