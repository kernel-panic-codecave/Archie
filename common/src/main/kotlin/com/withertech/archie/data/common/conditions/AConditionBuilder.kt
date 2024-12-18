package com.withertech.archie.data.common.conditions

import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation

object AConditionBuilder
{
	infix fun IACondition.and(other: IACondition): IACondition = AAndCondition(this, other)
	infix fun IACondition.or(other: IACondition): IACondition = AOrCondition(this, other)
	infix fun IACondition.xor(other: IACondition): IACondition = AXorCondition(this, other)
	infix fun IACondition.eql(other: IACondition): IACondition = AEqualsCondition(this, other)

	infix fun IACondition.nand(other: IACondition): IACondition = !(this and other)
	infix fun IACondition.nor(other: IACondition): IACondition = !(this or other)
	infix fun IACondition.xnor(other: IACondition): IACondition = !(this xor other)
	infix fun IACondition.neql(other: IACondition): IACondition = !(this eql other)

	fun and(vararg values: IACondition): IACondition = AAndCondition(*values)
	fun or(vararg values: IACondition): IACondition = AOrCondition(*values)
	fun xor(vararg values: IACondition): IACondition = AXorCondition(*values)
	fun eql(vararg values: IACondition): IACondition = AEqualsCondition(*values)

	fun nand(vararg values: IACondition): IACondition = !and(*values)
	fun nor(vararg values: IACondition): IACondition = !or(*values)
	fun xnor(vararg values: IACondition): IACondition = !xor(*values)
	fun neql(vararg values: IACondition): IACondition = !eql(*values)

	operator fun IACondition.plus(other: IACondition): IACondition = AAndCondition(this, other)
	operator fun IACondition.times(other: IACondition): IACondition = AOrCondition(this, other)
	operator fun IACondition.rem(other: IACondition): IACondition = AXorCondition(this, other)
	operator fun IACondition.minus(other: IACondition): IACondition = AEqualsCondition(this, other)

	operator fun IACondition.not(): IACondition = ANotCondition(this)

	val TRUE = ATrueCondition
	val FALSE = AFalseCondition

	fun mod(vararg mods: String): IACondition = AModLoadedCondition(*mods)
	fun registry(registry: ResourceKey<out Registry<*>>, vararg entries: ResourceLocation): IACondition = ARegistryCondition(registry.location(), *entries)
	fun registry(registry: Registry<*>, vararg entries: ResourceLocation): IACondition = ARegistryCondition(registry.key().location(), *entries)
	fun platform(platform: String): IACondition = APlatformCondition(platform)

	const val FABRIC = "fabric"
	const val FORGE = "forge"
	const val NEOFORGE = "neoforge"
}