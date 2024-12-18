package com.withertech.archie.serialization

import com.mojang.datafixers.util.Pair
import com.mojang.serialization.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import net.minecraft.nbt.NbtOps
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.peanuuutz.tomlkt.Toml

class SerializerCodec<X>(private val serializer: KSerializer<X>) : Codec<X>
{
	override fun <T : Any> encode(input: X, ops: DynamicOps<T>, prefix: T): DataResult<T>
	{
		return DataResult.success(when (ops)
		{
			is JsonOps, is KOps.Json -> {
				val element = Json.encodeToJsonElement(serializer, input)
				KOps.Json.convertTo(ops, element)
			}
			is KOps.Toml -> {
				val element = Toml.encodeToTomlElement(serializer, input)
				KOps.Toml.convertTo(ops as DynamicOps<T>, element)
			}
			is NbtOps, is KOps.Nbt -> {
				val element = NBT.encodeToNbtTagRootless(serializer, input)
				KOps.Nbt.convertTo(ops, element)
			}
			else -> throw UnsupportedOperationException()
		})
	}

	override fun <T> decode(ops: DynamicOps<T>, input: T): DataResult<Pair<X, T>>
	{
		return DataResult.success(Pair.of(when (ops)
		{
			is JsonOps, is KOps.Json -> {
				Json.decodeFromJsonElement(serializer, ops.convertTo(KOps.Json, input))
			}
			is KOps.Toml -> {
				Toml.decodeFromTomlElement(serializer, ops.convertTo(KOps.Toml, input))
			}
			is NbtOps, is KOps.Nbt -> {
				NBT.decodeFromNbtTagRootless(serializer, ops.convertTo(KOps.Nbt, input))
			}
			else -> throw UnsupportedOperationException()
		}, input))
	}
}

//@OptIn(ExperimentalSerializationApi::class)
//open class KotlinMapCodec<X>(private val serializer: KSerializer<X>) : MapCodec<X>()
//{
//	override fun <T> encode(input: X, ops: DynamicOps<T>, prefix: RecordBuilder<T>): RecordBuilder<T>
//	{
//		val element = NBT.encodeToNbtTagRootless(serializer, input)
//		require(element is NbtCompound) { "Class ${serializer.descriptor.serialName} does not serialize to a CompoundTag!" }
//		return prefix.apply {
//			element.nbtCompound.forEach { (key, value) ->
//				add(key, NbtOps.INSTANCE.convertTo(ops, value.toMinecraft))
//			}
//		}
//	}
//
//	override fun <T> keys(ops: DynamicOps<T>): Stream<T> =
//		serializer.descriptor.elementNames.map { ops.createString(it) }.stream()
//
//	override fun <T> decode(ops: DynamicOps<T>, input: MapLike<T>): DataResult<X>
//	{
//		val element = NBT.decodeFromNbtTagRootless(serializer, NbtCompound(input.entries().toList().associate {
//			ops.convertTo(NbtOps.INSTANCE, it.first).fromMinecraft!!.nbtString.value to ops.convertTo(
//				NbtOps.INSTANCE,
//				it.second
//			).fromMinecraft!!
//		}))
//		return DataResult.success(element)
//	}
//}

inline fun <reified T : Any> codec(): Codec<T> = serializer<T>().codec()
inline fun <reified T : Any> streamCodec(): StreamCodec<RegistryFriendlyByteBuf, T> = serializer<T>().getStreamCodec()

//inline fun <reified T> mapCodec(): MapCodec<T> = serializer<T>().mapCodec()
inline fun <reified T : Any> KSerializer<T>.codec(): Codec<T> = SerializerCodec(this)

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T : Any> KSerializer<T>.getStreamCodec(): StreamCodec<RegistryFriendlyByteBuf, T> = StreamCodec.of(
	{ buffer, value -> buffer.writeByteArray(Cbor.encodeToByteArray(this, value)) },
	{ buffer -> Cbor.decodeFromByteArray(this, buffer.readByteArray()) }
)
//inline fun <reified T> KSerializer<T>.mapCodec(): MapCodec<T> = KotlinMapCodec(this)
