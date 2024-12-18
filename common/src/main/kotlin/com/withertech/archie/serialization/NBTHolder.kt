package com.withertech.archie.serialization

import com.withertech.archie.transfer.ArchieItemStorage
import dev.architectury.fluid.FluidStack
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.serializer
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty

@Suppress("unused")
interface NBTHolder
{
	fun <T> field(serializer: KSerializer<T>, default: () -> T): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, T>>

	fun itemField(size: Int): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, ArchieItemStorage>>

	fun booleanField(default: () -> Boolean = { false }): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, Boolean>> = field(Boolean.serializer(), default)
	fun byteField(default: () -> Byte = { 0 }): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, Byte>> = field(Byte.serializer(), default)
	fun ubyteField(default: () -> UByte = { 0u }): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, UByte>> = field(UByte.serializer(), default)
	fun shortField(default: () -> Short = { 0 }): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, Short>> = field(Short.serializer(), default)
	fun ushortField(default: () -> UShort = { 0u }): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, UShort>> = field(UShort.serializer(), default)
	fun intField(default: () -> Int = { 0 }): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, Int>> = field(Int.serializer(), default)
	fun uintField(default: () -> UInt = { 0u }): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, UInt>> = field(UInt.serializer(), default)
	fun longField(default: () -> Long = { 0 }): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, Long>> = field(Long.serializer(), default)
	fun ulongField(default: () -> ULong = { 0u }): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, ULong>> = field(ULong.serializer(), default)
	fun floatField(default: () -> Float = { 0.0f }): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, Float>> = field(Float.serializer(), default)
	fun doubleField(default: () -> Double = { 0.0 }): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, Double>> = field(Double.serializer(), default)
	fun stringField(default: () -> String = { "" }): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, String>> = field(String.serializer(), default)

	fun loadFromTag(compoundTag: CompoundTag)

	fun saveToTag(compoundTag: CompoundTag)

	fun getSyncTag(): CompoundTag


	companion object
	{
		fun create(): NBTHolder = NBTHolderImpl()

		fun item(stack: ItemStack): NBTHolder = ItemStackNBTHolderImpl(stack)

		fun <R> item(stack: ItemStack, block: NBTHolder.() -> R): R
		{
			return item(stack).block()
		}

		fun fluid(stack: FluidStack): NBTHolder = FluidStackNBTHolderImpl(stack)

		fun <R> fluid(stack: FluidStack, block: NBTHolder.() -> R): R
		{
			return fluid(stack).block()
		}
	}
}

inline fun <reified T> NBTHolder.field(noinline default: () -> T): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, T>> = field(serializer<T>(), default)