package com.withertech.archie.data.common.conditions

import net.minecraft.data.recipes.RecipeOutput

fun RecipeOutput.withCondition(condition: ICondition): RecipeOutput
{
	return withCondition { condition }
}

fun RecipeOutput.withCondition(block: ConditionBuilder.() -> ICondition): RecipeOutput
{
	return ConditionsPlatform.withCondition(this, ConditionBuilder.block())
}