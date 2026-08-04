package net.kernelpanicsoft.archie.data.common.conditions

import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation

/**
 * DSL for building [IACondition] trees with infix/operator combinators (`and`, `or`, `xor`,
 * `eql`, their negated `n*` counterparts, and `!` operator aliases) plus factory
 * functions for the leaf conditions ([mod], [registry], [platform], [TRUE], [FALSE]).
 *
 * Import the members (`import ...AConditionBuilder.*`) to write conditions like
 * `mod("architectury") and platform(FABRIC)`.
 */
object AConditionBuilder
{
	/** [AAndCondition] of `this` and [other]. */


	fun and(vararg values: IACondition): IACondition = AAndCondition(*values)
	fun or(vararg values: IACondition): IACondition = AOrCondition(*values)
	fun xor(vararg values: IACondition): IACondition = AXorCondition(*values)
	fun eql(vararg values: IACondition): IACondition = AEqualsCondition(*values)

	fun nand(vararg values: IACondition): IACondition = !and(*values)
	fun nor(vararg values: IACondition): IACondition = !or(*values)
	fun xnor(vararg values: IACondition): IACondition = !xor(*values)
	fun neql(vararg values: IACondition): IACondition = !eql(*values)

	infix fun IACondition.and(other: IACondition): IACondition = and(this, other)
	infix fun IACondition.or(other: IACondition): IACondition = or(this, other)
	infix fun IACondition.xor(other: IACondition): IACondition = xor(this, other)
	infix fun IACondition.eql(other: IACondition): IACondition = eql(this, other)

	infix fun IACondition.nand(other: IACondition): IACondition = !(this and other)
	infix fun IACondition.nor(other: IACondition): IACondition = !(this or other)
	infix fun IACondition.xnor(other: IACondition): IACondition = !(this xor other)
	infix fun IACondition.neql(other: IACondition): IACondition = !(this eql other)

	operator fun IACondition.not(): IACondition = ANotCondition(this)

	/** Always-true condition; see [ATrueCondition]. */
	val TRUE = ATrueCondition

	/** Always-false condition; see [AFalseCondition]. */
	val FALSE = AFalseCondition

	/** Condition that holds when every mod id in [mods] is loaded. */
	fun mod(vararg mods: String): IACondition = AModLoadedCondition(*mods)

	/** Condition that holds when every one of [entries] is registered in [registry]. */
	fun registry(registry: ResourceKey<out Registry<*>>, vararg entries: ResourceLocation): IACondition = ARegistryCondition(registry.location(), *entries)

	/** Condition that holds when every one of [entries] is registered in [registry]. */
	fun registry(registry: Registry<*>, vararg entries: ResourceLocation): IACondition = ARegistryCondition(registry.key().location(), *entries)

	/** Condition that holds when the running loader's platform id equals [platform]; see [FABRIC]/[NEOFORGE]. */
	fun platform(platform: String): IACondition = APlatformCondition(platform)

	const val FABRIC = "fabric"
	const val NEOFORGE = "neoforge"
}