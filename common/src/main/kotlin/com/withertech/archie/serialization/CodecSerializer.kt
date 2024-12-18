package com.withertech.archie.serialization

import com.mojang.serialization.Codec
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import net.benwoodworth.knbt.NbtDecoder
import net.benwoodworth.knbt.NbtEncoder
import net.benwoodworth.knbt.NbtTag
import net.peanuuutz.tomlkt.TomlDecoder
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlEncoder

open class CodecSerializer<T>(private val codec: Codec<T>) : KSerializer<T>
{
	@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
	override val descriptor: SerialDescriptor = buildSerialDescriptor("CodecSerializer", PolymorphicKind.SEALED) {
		element("JsonElement", defer { JsonElement.serializer().descriptor })
		element("TomlElement", defer { TomlElement.serializer().descriptor })
		element("NbtTag", defer { NbtTag.serializer().descriptor })
	}

	override fun deserialize(decoder: Decoder): T
	{
		return when (decoder)
		{
			is JsonDecoder -> codec.parse(KOps.Json, decoder.decodeJsonElement()).orThrow
			is TomlDecoder -> codec.parse(KOps.Toml, decoder.decodeTomlElement()).orThrow
			is NbtDecoder -> codec.parse(KOps.Nbt, decoder.decodeNbtTag()).orThrow
			else -> throw UnsupportedOperationException()
		}
	}

	override fun serialize(encoder: Encoder, value: T)
	{
		when (encoder)
		{
			is JsonEncoder -> encoder.encodeJsonElement(codec.encodeStart(KOps.Json, value).orThrow)
			is TomlEncoder -> encoder.encodeTomlElement(codec.encodeStart(KOps.Toml, value).orThrow)
			is NbtEncoder -> encoder.encodeNbtTag(codec.encodeStart(KOps.Nbt, value).orThrow)
			else -> throw UnsupportedOperationException()
		}
	}
}

inline fun <reified T> Codec<T>.serializer(): KSerializer<T> = CodecSerializer(this)