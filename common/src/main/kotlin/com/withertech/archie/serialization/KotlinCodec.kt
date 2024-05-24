package com.withertech.archie.serialization

import com.mojang.datafixers.util.Pair
import com.mojang.serialization.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.capturedKClass
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.serializer
import net.benwoodworth.knbt.*
import net.minecraft.nbt.NbtOps
import java.util.stream.Stream

open class KotlinCodec<X>(private val serializer: KSerializer<X>) : Codec<X>
{
	override fun <T> encode(input: X, ops: DynamicOps<T>, prefix: T): DataResult<T>
	{
		val element = NBT.encodeToNbtTagRootless(serializer, input)
		return DataResult.success(NbtOps.INSTANCE.convertTo(ops, element.toMinecraft))
	}

	override fun <T> decode(ops: DynamicOps<T>, input: T): DataResult<Pair<X, T>>
	{
		val element = NBT.decodeFromNbtTagRootless(serializer, ops.convertTo(NbtOps.INSTANCE, input).fromMinecraft!!)
		return DataResult.success(Pair.of(element, ops.empty()))
	}
}

@OptIn(ExperimentalSerializationApi::class)
open class KotlinMapCodec<X>(private val serializer: KSerializer<X>) : MapCodec<X>()
{
	override fun <T> encode(input: X, ops: DynamicOps<T>, prefix: RecordBuilder<T>): RecordBuilder<T>
	{
		val element = NBT.encodeToNbtTagRootless(serializer, input)
		require(element is NbtCompound) { "Class ${serializer.descriptor.serialName} does not serialize to a CompoundTag!" }
		return ops.mapBuilder().apply {
			element.nbtCompound.forEach { (key, value) ->
				add(key, NbtOps.INSTANCE.convertTo(ops, value.toMinecraft))
			}
		}
	}

	override fun <T> keys(ops: DynamicOps<T>): Stream<T> =
		serializer.descriptor.elementNames.map { ops.createString(it) }.stream()

	override fun <T> decode(ops: DynamicOps<T>, input: MapLike<T>): DataResult<X>
	{
		val element = NBT.decodeFromNbtTagRootless(serializer, NbtCompound(input.entries().toList().associate {
			ops.convertTo(NbtOps.INSTANCE, it.first).fromMinecraft!!.nbtString.value to ops.convertTo(
				NbtOps.INSTANCE,
				it.second
			).fromMinecraft!!
		}))
		return DataResult.success(element)
	}
}

inline fun <reified T> codec(): Codec<T> = KotlinCodec(serializer())
inline fun <reified T> mapCodec(): MapCodec<T> = KotlinMapCodec(serializer())
inline fun <reified T> KSerializer<T>.codec(): Codec<T> = KotlinCodec(this)
inline fun <reified T> KSerializer<T>.mapCodec(): MapCodec<T> = KotlinMapCodec(this)
