package com.withertech.archie.serialization

import com.mojang.blaze3d.platform.InputConstants.Type
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import me.shedaniel.clothconfig2.api.Modifier
import me.shedaniel.clothconfig2.api.ModifierKeyCode
import me.shedaniel.math.Color
import net.minecraft.resources.ResourceLocation

object ResourceLocationSerializer : KSerializer<ResourceLocation>
{
	override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ResourceLocation", PrimitiveKind.STRING)

	override fun deserialize(decoder: Decoder): ResourceLocation
	{
		return ResourceLocation(decoder.decodeString())
	}

	override fun serialize(encoder: Encoder, value: ResourceLocation)
	{
		encoder.encodeString(value.toString())
	}

}
typealias SerializableResourceLocation = @Serializable(with = ResourceLocationSerializer::class) ResourceLocation

object ModifierKeyCodeSerializer : KSerializer<ModifierKeyCode>
{
	@Serializable
	enum class KeyType(val type: Type)
	{
		KEYSYM(Type.KEYSYM),
		SCANCODE(Type.KEYSYM),
		MOUSE(Type.MOUSE);

		companion object
		{
			fun forType(type: Type): KeyType
			{
				return when (type)
				{
					Type.KEYSYM -> KEYSYM
					Type.SCANCODE -> SCANCODE
					Type.MOUSE -> MOUSE
				}
			}
		}
	}

	override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ModifierKeyCode")
	{
		element<KeyType>("type")
		element<Int>("key_code")
		element<Short>("modifier", isOptional = true)
	}

	override fun deserialize(decoder: Decoder): ModifierKeyCode
	{
		return decoder.decodeStructure(descriptor)
		{
			var type: KeyType = KeyType.KEYSYM
			var keyCode = 0
			var modifier: Short = 0
			while (true)
			{
				when (val index = decodeElementIndex(descriptor))
				{
					0 -> type = decodeSerializableElement(descriptor, index, KeyType.serializer())
					1 -> keyCode = decodeIntElement(descriptor, index)
					2 -> modifier = decodeShortElement(descriptor, index)
					CompositeDecoder.DECODE_DONE -> break
					else -> error("Unexpected index: $index")
				}
			}
			if (keyCode == -1)
				ModifierKeyCode.unknown()
			ModifierKeyCode.of(type.type.getOrCreate(keyCode), Modifier.of(modifier))
		}
	}

	override fun serialize(encoder: Encoder, value: ModifierKeyCode)
	{
		encoder.encodeStructure(descriptor)
		{
			encodeSerializableElement(descriptor, 0, KeyType.serializer(), KeyType.forType(value.type))
			encodeIntElement(descriptor, 1, value.keyCode.value)
			if (!value.modifier.isEmpty)
				encodeShortElement(descriptor, 2, value.modifier.value)
		}
	}

}

object ColorSerializer : KSerializer<Color>
{
	override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)

	override fun deserialize(decoder: Decoder): Color
	{
		return Color.ofTransparent(
			decoder.decodeString()
				.trimStart('#')
				.toLong(16)
				.toInt()
		)
	}

	override fun serialize(encoder: Encoder, value: Color)
	{
		encoder.encodeString("#${Integer.toHexString(value.color).padStart(8, '0')}")
	}

}