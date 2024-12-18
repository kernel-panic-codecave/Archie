package com.withertech.archie.data.common.conditions

abstract class AGroupCondition : IACondition
{
	abstract val children: List<IACondition>
	abstract fun reducer(a: Boolean, b: Boolean): Boolean

	override fun test(context: IACondition.IContext): Boolean =
		children.map { it.test(context) }.reduce(::reducer)
}