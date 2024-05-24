package com.withertech.archie.config

import com.withertech.archie.serialization.*
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
import me.shedaniel.clothconfig2.api.ModifierKeyCode
import me.shedaniel.math.Color
import net.minecraft.resources.ResourceLocation
import kotlin.reflect.KClass

internal sealed class FieldType<T>
{
	abstract val serializer: KSerializer<T>

	data class Category(val category: CategorySpec) : FieldType<CategorySpec>()
	{
		override val serializer: KSerializer<CategorySpec> = category.serializer
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

	data class Spec(val factory: () -> CategorySpec) : FieldType<CategorySpec>()
	{
		override val serializer: KSerializer<CategorySpec> = CategorySpec.ConfigCategorySerializer(factory)
	}

	data object Registry : FieldType<ResourceLocation>()
	{
		override val serializer: KSerializer<ResourceLocation> = ResourceLocationSerializer
	}

	data object KeyCode : FieldType<ModifierKeyCode>()
	{
		override val serializer: KSerializer<ModifierKeyCode> = ModifierKeyCodeSerializer
	}

	data object Color : FieldType<me.shedaniel.math.Color>()
	{
		override val serializer: KSerializer<me.shedaniel.math.Color> = ColorSerializer
	}

	data class EnumSelector<T : Enum<T>>(val kClass: KClass<T>) : FieldType<T>()
	{
		@OptIn(InternalSerializationApi::class)
		override val serializer: KSerializer<T> = kClass.serializer()
	}

	data class Selector<T : Any>(val kClass: KClass<T>) : FieldType<T>()
	{
		@OptIn(InternalSerializationApi::class)
		override val serializer: KSerializer<T> = kClass.serializer()
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

	data class SpecList(val factory: () -> CategorySpec) : FieldType<List<CategorySpec>>()
	{
		override val serializer: KSerializer<List<CategorySpec>> = DeferredListSerializer(CategorySpec.ConfigCategorySerializer(factory))
	}

	data object RegistryList : FieldType<List<ResourceLocation>>()
	{
		override val serializer: KSerializer<List<ResourceLocation>> = ListSerializer(ResourceLocationSerializer)
	}

	data object KeyCodeList : FieldType<List<ModifierKeyCode>>()
	{
		override val serializer: KSerializer<List<ModifierKeyCode>> = ListSerializer(ModifierKeyCodeSerializer)
	}

	data object ColorList : FieldType<List<me.shedaniel.math.Color>>()
	{
		override val serializer: KSerializer<List<me.shedaniel.math.Color>> = ListSerializer(ColorSerializer)
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

	data class SpecMap(val factory: () -> CategorySpec) : FieldType<Map<kotlin.String, CategorySpec>>()
	{
		override val serializer: KSerializer<Map<kotlin.String, CategorySpec>> = DeferredMapSerializer(kotlin.String.serializer(), CategorySpec.ConfigCategorySerializer(factory))
	}

	data object RegistryMap : FieldType<Map<kotlin.String, ResourceLocation>>()
	{
		override val serializer: KSerializer<Map<kotlin.String, ResourceLocation>> = MapSerializer(kotlin.String.serializer(), ResourceLocationSerializer)
	}

	data object KeyCodeMap : FieldType<Map<kotlin.String, ModifierKeyCode>>()
	{
		override val serializer: KSerializer<Map<kotlin.String, ModifierKeyCode>> = MapSerializer(kotlin.String.serializer(), ModifierKeyCodeSerializer)
	}

	data object ColorMap : FieldType<Map<kotlin.String, me.shedaniel.math.Color>>()
	{
		override val serializer: KSerializer<Map<kotlin.String, me.shedaniel.math.Color>> = MapSerializer(kotlin.String.serializer(), ColorSerializer)
	}
}