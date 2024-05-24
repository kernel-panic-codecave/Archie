package com.withertech.archie.serialization

import com.google.gson.Gson
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import net.benwoodworth.knbt.NbtTag
import net.benwoodworth.knbt.asNbtDecoder
import net.benwoodworth.knbt.asNbtEncoder
import net.minecraft.nbt.NbtOps

open class NbtCodecSerializer<T>(private val codec: Codec<T>) : KSerializer<T>
{
	override val descriptor: SerialDescriptor
		get() = NbtTag.serializer().descriptor

	override fun deserialize(decoder: Decoder): T
	{

		return codec.parse(NbtOps.INSTANCE, decoder.asNbtDecoder().decodeNbtTag().toMinecraft).orThrow
	}

	override fun serialize(encoder: Encoder, value: T)
	{
		encoder.asNbtEncoder().encodeNbtTag(codec.encodeStart(NbtOps.INSTANCE, value).orThrow.fromMinecraft!!)
	}
}

open class JsonCodecSerializer<T>(private val codec: Codec<T>) : KSerializer<T>
{
	override val descriptor: SerialDescriptor
		get() = JsonElement.serializer().descriptor

	override fun deserialize(decoder: Decoder): T
	{
		return codec.parse(JsonOps.INSTANCE, Gson().fromJson(Json.encodeToString(decoder.decodeSerializableValue(JsonElement.serializer())), com.google.gson.JsonElement::class.java)).orThrow
	}

	override fun serialize(encoder: Encoder, value: T)
	{
		encoder.encodeSerializableValue(JsonElement.serializer(), Json.parseToJsonElement(Gson().toJson(codec.encodeStart(JsonOps.INSTANCE, value).orThrow)))
	}
}

inline fun <reified T> Codec<T>.nbtSerializer(): KSerializer<T> = NbtCodecSerializer(this)
inline fun <reified T> Codec<T>.jsonSerializer(): KSerializer<T> = JsonCodecSerializer(this)