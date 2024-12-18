package com.withertech.archie.data.common.conditions

import net.minecraft.data.recipes.RecipeOutput

fun RecipeOutput.withCondition(condition: IACondition): RecipeOutput
{
	return withCondition { condition }
}

fun RecipeOutput.withCondition(block: AConditionBuilder.() -> IACondition): RecipeOutput
{
	return AConditionsPlatform.withCondition(this, AConditionBuilder.block())
}

inline fun buildCondition(block: AConditionBuilder.() -> IACondition): IACondition = AConditionBuilder.block()