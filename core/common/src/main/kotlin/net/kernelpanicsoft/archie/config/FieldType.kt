package net.kernelpanicsoft.archie.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
import net.kernelpanicsoft.archie.serialization.DeferredListSerializer
import net.kernelpanicsoft.archie.serialization.DeferredMapSerializer
import net.kernelpanicsoft.archie.gui.util.KColor
import net.kernelpanicsoft.archie.serialization.SerializationManager
import net.kernelpanicsoft.archie.serialization.serializers.ResourceLocationSerializer
import net.minecraft.resources.ResourceLocation
import kotlin.reflect.KClass
import kotlin.reflect.full.createType

/**
 * Tags a [DataSpec] field with its runtime type and the [KSerializer] used to read/write it,
 * so [DataSpec.ConfigCategorySerializer] can (de)serialize each field generically without a
 * `when` over the raw value type. One subtype per builder function in [DataSpec] (e.g.
 * [Boolean] for `boolean()`, [IntList] for `intList()`).
 */
internal sealed class FieldType<T>
{
	abstract val serializer: KSerializer<T>

	data class Category(val category: DataSpec) : FieldType<DataSpec>()
	{
		override val serializer: KSerializer<DataSpec> = category.serializer
	}

	data object Boolean : FieldType<kotlin.Boolean>()
	{
		override val serializer: KSerializer<kotlin.Boolean> = kotlin.Boolean.serializer()
	}

	data object Int : FieldType<kotlin.Int>()
	{
		override val serializer: KSerializer<kotlin.Int> = kotlin.Int.serializer()
	}

	data object Long : FieldType<kotlin.Long>()
	{
		override val serializer: KSerializer<kotlin.Long> = kotlin.Long.serializer()
	}

	data object Float : FieldType<kotlin.Float>()
	{
		override val serializer: KSerializer<kotlin.Float> = kotlin.Float.serializer()
	}

	data object Double : FieldType<kotlin.Double>()
	{
		override val serializer: KSerializer<kotlin.Double> = kotlin.Double.serializer()
	}

	data object String : FieldType<kotlin.String>()
	{
		override val serializer: KSerializer<kotlin.String> = kotlin.String.serializer()
	}

	data class Spec(val factory: () -> DataSpec) : FieldType<DataSpec>()
	{
		override val serializer: KSerializer<DataSpec> =
			DataSpec.ConfigCategorySerializer(factory)
	}

	data object Registry : FieldType<ResourceLocation>()
	{
		override val serializer: KSerializer<ResourceLocation> = ResourceLocationSerializer
	}

	data object KeyCode : FieldType<CommonKeyCode>()
	{
		override val serializer: KSerializer<CommonKeyCode> = CommonKeyCode.serializer()
	}

	data object Color : FieldType<KColor>()
	{
		override val serializer: KSerializer<KColor> = KColor.serializer()
	}

	data class EnumSelector<T : Enum<T>>(val kClass: KClass<T>) : FieldType<T>()
	{
		// Module-aware, unlike the bare KClass.serializer() reflective lookup - matters if T
		// (or a field of it) ever needs a contextual serializer registered via SerializationManager.
		@Suppress("UNCHECKED_CAST")
		override val serializer: KSerializer<T> = SerializationManager.module.serializer(kClass.createType()) as KSerializer<T>
	}

	data class Selector<T : Any>(val kClass: KClass<T>) : FieldType<T>()
	{
		@Suppress("UNCHECKED_CAST")
		override val serializer: KSerializer<T> = SerializationManager.module.serializer(kClass.createType()) as KSerializer<T>
	}

	/** A field that stores one of [options] (a [ConfigSpec]) by its [ConfigSpec.id] rather than its data. See [DataSpec.configRef]. */
	data class ConfigRef<T : ConfigSpec>(val options: List<T>, val default: T) : FieldType<T>()
	{
		override val serializer: KSerializer<T> = object : KSerializer<T>
		{
			override val descriptor = PrimitiveSerialDescriptor("ConfigRef", PrimitiveKind.STRING)
			override fun serialize(encoder: Encoder, value: T) = encoder.encodeString(value.id)
			override fun deserialize(decoder: Decoder): T
			{
				val id = decoder.decodeString()
				return options.find { it.id == id } ?: default
			}
		}
	}

	data object IntList : FieldType<List<kotlin.Int>>()
	{
		override val serializer: KSerializer<List<kotlin.Int>> = ListSerializer(kotlin.Int.serializer())
	}

	data object LongList : FieldType<List<kotlin.Long>>()
	{
		override val serializer: KSerializer<List<kotlin.Long>> = ListSerializer(kotlin.Long.serializer())
	}

	data object FloatList : FieldType<List<kotlin.Float>>()
	{
		override val serializer: KSerializer<List<kotlin.Float>> = ListSerializer(kotlin.Float.serializer())
	}

	data object DoubleList : FieldType<List<kotlin.Double>>()
	{
		override val serializer: KSerializer<List<kotlin.Double>> = ListSerializer(kotlin.Double.serializer())
	}

	data object StringList : FieldType<List<kotlin.String>>()
	{
		override val serializer: KSerializer<List<kotlin.String>> = ListSerializer(kotlin.String.serializer())
	}

	data class SpecList(val factory: () -> DataSpec) : FieldType<List<DataSpec>>()
	{
		override val serializer: KSerializer<List<DataSpec>> = DeferredListSerializer(
			DataSpec.ConfigCategorySerializer(factory)
		)
	}

	data object RegistryList : FieldType<List<ResourceLocation>>()
	{
		override val serializer: KSerializer<List<ResourceLocation>> = ListSerializer(ResourceLocationSerializer)
	}

	data object KeyCodeList : FieldType<List<CommonKeyCode>>()
	{
		override val serializer: KSerializer<List<CommonKeyCode>> = ListSerializer(
			CommonKeyCode.serializer())
	}

	data object ColorList : FieldType<List<KColor>>()
	{
		override val serializer: KSerializer<List<KColor>> = ListSerializer(KColor.serializer())
	}

	data object IntMap : FieldType<Map<kotlin.String, kotlin.Int>>()
	{
		override val serializer: KSerializer<Map<kotlin.String, kotlin.Int>> = MapSerializer(kotlin.String.serializer(), kotlin.Int.serializer())
	}

	data object LongMap : FieldType<Map<kotlin.String, kotlin.Long>>()
	{
		override val serializer: KSerializer<Map<kotlin.String, kotlin.Long>> = MapSerializer(kotlin.String.serializer(), kotlin.Long.serializer())
	}

	data object FloatMap : FieldType<Map<kotlin.String, kotlin.Float>>()
	{
		override val serializer: KSerializer<Map<kotlin.String, kotlin.Float>> = MapSerializer(kotlin.String.serializer(), kotlin.Float.serializer())
	}

	data object DoubleMap : FieldType<Map<kotlin.String, kotlin.Double>>()
	{
		override val serializer: KSerializer<Map<kotlin.String, kotlin.Double>> = MapSerializer(kotlin.String.serializer(), kotlin.Double.serializer())
	}

	data object StringMap : FieldType<Map<kotlin.String, kotlin.String>>()
	{
		override val serializer: KSerializer<Map<kotlin.String, kotlin.String>> = MapSerializer(kotlin.String.serializer(), kotlin.String.serializer())
	}

	data class SpecMap(val factory: () -> DataSpec) : FieldType<Map<kotlin.String, DataSpec>>()
	{
		override val serializer: KSerializer<Map<kotlin.String, DataSpec>> = DeferredMapSerializer(kotlin.String.serializer(),
			DataSpec.ConfigCategorySerializer(factory)
		)
	}

	data object RegistryMap : FieldType<Map<kotlin.String, ResourceLocation>>()
	{
		override val serializer: KSerializer<Map<kotlin.String, ResourceLocation>> = MapSerializer(kotlin.String.serializer(), ResourceLocationSerializer)
	}

	data object KeyCodeMap : FieldType<Map<kotlin.String, CommonKeyCode>>()
	{
		override val serializer: KSerializer<Map<kotlin.String, CommonKeyCode>> = MapSerializer(kotlin.String.serializer(), CommonKeyCode.serializer())
	}

	data object ColorMap : FieldType<Map<kotlin.String, KColor>>()
	{
		override val serializer: KSerializer<Map<kotlin.String, KColor>> = MapSerializer(kotlin.String.serializer(), KColor.serializer())
	}
}