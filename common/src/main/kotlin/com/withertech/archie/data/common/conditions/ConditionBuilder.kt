package com.withertech.archie.data.common.conditions

import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation

object ConditionBuilder
{
	infix fun ICondition.and(other: ICondition): ICondition = AndCondition(this, other)
	infix fun ICondition.or(other: ICondition): ICondition = OrCondition(this, other)
	infix fun ICondition.xor(other: ICondition): ICondition = XorCondition(this, other)
	infix fun ICondition.eql(other: ICondition): ICondition = EqualsCondition(this, other)

	infix fun ICondition.nand(other: ICondition): ICondition = !(this and other)
	infix fun ICondition.nor(other: ICondition): ICondition = !(this or other)
	infix fun ICondition.xnor(other: ICondition): ICondition = !(this xor other)
	infix fun ICondition.neql(other: ICondition): ICondition = !(this eql other)

	fun and(vararg values: ICondition): ICondition = AndCondition(*values)
	fun or(vararg values: ICondition): ICondition = OrCondition(*values)
	fun xor(vararg values: ICondition): ICondition = XorCondition(*values)
	fun eql(vararg values: ICondition): ICondition = EqualsCondition(*values)

	fun nand(vararg values: ICondition): ICondition = !and(*values)
	fun nor(vararg values: ICondition): ICondition = !or(*values)
	fun xnor(vararg values: ICondition): ICondition = !xor(*values)
	fun neql(vararg values: ICondition): ICondition = !eql(*values)

	operator fun ICondition.plus(other: ICondition): ICondition = AndCondition(this, other)
	operator fun ICondition.times(other: ICondition): ICondition = OrCondition(this, other)
	operator fun ICondition.rem(other: ICondition): ICondition = XorCondition(this, other)
	operator fun ICondition.minus(other: ICondition): ICondition = EqualsCondition(this, other)

	operator fun ICondition.not(): ICondition = NotCondition(this)

	val TRUE = TrueCondition
	val FALSE = FalseCondition

	fun mod(vararg mods: String): ICondition = ModLoadedCondition(*mods)
	fun registry(registry: ResourceKey<out Registry<*>>, vararg entries: ResourceLocation): ICondition = RegistryCondition(registry.location(), *entries)
	fun registry(registry: Registry<*>, vararg entries: ResourceLocation): ICondition = RegistryCondition(registry.key().location(), *entries)
	fun platform(platform: String): ICondition = PlatformCondition(platform)

	const val FABRIC = "fabric"
	const val FORGE = "forge"
	const val NEOFORGE = "neoforge"
}