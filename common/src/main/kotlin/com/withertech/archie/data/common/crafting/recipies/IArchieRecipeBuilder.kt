package com.withertech.archie.data.common.crafting.recipies

import com.withertech.archie.data.common.conditions.ConditionBuilder
import com.withertech.archie.data.common.conditions.ICondition
import com.withertech.archie.data.common.conditions.withCondition
import net.minecraft.data.recipes.RecipeBuilder
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.resources.ResourceLocation

interface IArchieRecipeBuilder : RecipeBuilder
{
	fun save(recipeOutput: RecipeOutput, id: ResourceLocation? = null, condition: ConditionBuilder.() -> ICondition)
	{
		if (id != null)
			save(recipeOutput.withCondition(condition), id)
		else
			save(recipeOutput.withCondition(condition))
	}
}