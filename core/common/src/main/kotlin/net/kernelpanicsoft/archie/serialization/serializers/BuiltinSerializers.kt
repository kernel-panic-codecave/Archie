package net.kernelpanicsoft.archie.serialization.serializers

import com.mojang.blaze3d.platform.InputConstants.Type
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.SerializersModule
import me.shedaniel.clothconfig2.api.Modifier
import me.shedaniel.clothconfig2.api.ModifierKeyCode
import me.shedaniel.math.Color
import net.kernelpanicsoft.archie.util.isClothConfigLoaded
import net.kernelpanicsoft.archie.util.onClient

/* ------------------ TypeAliases ------------------ */

/**
 * Contextual type-alias for Cloth Config's [ModifierKeyCode] that uses [ModifierKeyCodeSerializer]
 * when the field is annotated with `@Contextual`.
 */
typealias SModifierKeyCode = @Contextual ModifierKeyCode
/**
 * Contextual type-alias for Cloth Config's [Color] that uses [ColorSerializer] when the field
 * is annotated with `@Contextual`.
 */
typealias SColor = @Contextual Color

/* ------------------ Serializers ------------------ */

/** A [KSerializer] for Cloth Config's [ModifierKeyCode] (a keybind plus its held modifier). */
object ModifierKeyCodeSerializer : KSerializer<ModifierKeyCode>
{
	@Serializable
	enum class KeyType(val type: Type)
	{
		KEYSYM(Type.KEYSYM),
		SCANCODE(Type.SCANCODE),
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
			else
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

/** A [KSerializer] for Cloth Config's [Color] that encodes/decodes as an `#AARRGGBB` hex string. */
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

/**
 * A [SerializersModule] that registers Archie's Cloth Config-related type serializers
 * ([ModifierKeyCodeSerializer], [ColorSerializer]) as contextual serializers.
 *
 * [me.shedaniel.math.Color] is pulled in transitively through `cloth-config`, but Archie's own
 * config system exposes it directly in its own public API (unlike [ModifierKeyCode], which stays
 * behind [net.kernelpanicsoft.archie.config.CommonKeyCode]) - so `basic-math` (the small library
 * that actually declares [me.shedaniel.math.Color]) is bundled for real (see each module's own
 * `build.gradle.kts`) rather than left to Cloth Config's own optional presence, and
 * [ColorSerializer] is registered unconditionally on both sides. [ModifierKeyCode] has no such
 * bundled fallback - it's genuinely Cloth Config's own type, with no reason to touch it unless
 * Cloth Config's own UI integration is in play - so [ModifierKeyCodeSerializer] stays behind both
 * [onClient] (a dedicated server shouldn't class-load it) *and* [isClothConfigLoaded] (a client
 * without Cloth Config installed shouldn't either).
 */
val BuiltInSerializersModule = SerializersModule {
	contextual(Color::class, ColorSerializer)
	onClient {
		if (isClothConfigLoaded)
			contextual(ModifierKeyCode::class, ModifierKeyCodeSerializer)
	}
}