package net.kernelpanicsoft.archie.data.common.conditions

import net.minecraft.data.recipes.RecipeOutput

/** Attaches [condition] to the next recipe written to this [RecipeOutput]. */
fun RecipeOutput.withCondition(condition: IACondition): RecipeOutput
{
	return withCondition { condition }
}

/** Attaches the [IACondition] built by [block] (with [AConditionBuilder] in scope) to the next recipe written to this [RecipeOutput]. */
fun RecipeOutput.withCondition(block: AConditionBuilder.() -> IACondition): RecipeOutput
{
	return ADatagenConditionsPlatform.withCondition(this, AConditionBuilder.block())
}

/** Builds an [IACondition] with [AConditionBuilder] in scope, e.g. `buildCondition { mod("architectury") }`. */
inline fun buildCondition(block: AConditionBuilder.() -> IACondition): IACondition = AConditionBuilder.block()