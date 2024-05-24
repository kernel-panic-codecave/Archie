package com.withertech.archie.data.common.conditions

abstract class GroupCondition : ICondition
{
	abstract val children: List<ICondition>
	abstract fun reducer(a: Boolean, b: Boolean): Boolean

	override fun test(context: ICondition.IContext): Boolean =
		children.map { it.test(context) }.reduce(::reducer)
}