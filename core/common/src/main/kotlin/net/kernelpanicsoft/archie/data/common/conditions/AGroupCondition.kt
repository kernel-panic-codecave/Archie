package net.kernelpanicsoft.archie.data.common.conditions

/**
 * Base for [IACondition]s that combine [children] pairwise via [reducer], e.g. [AAndCondition],
 * [AOrCondition], [AXorCondition]. [children] must be non-empty.
 */
abstract class AGroupCondition : IACondition
{
	abstract val children: List<IACondition>

	/** Combines two child results into one; applied left-to-right across [children]. */
	abstract fun reducer(a: Boolean, b: Boolean): Boolean

	override fun test(context: IACondition.IContext): Boolean =
		children.map { it.test(context) }.reduce(::reducer)
}