package com.withertech.archie.data.common.crafting.ingredients

import com.mojang.serialization.*
import com.withertech.archie.data.common.crafting.ingredients.CustomIngredientPlatform.neoforge
import net.neoforged.neoforge.common.crafting.IngredientType
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import java.util.stream.Stream

actual object CustomIngredientSerializerPlatform
{
	actual fun register(serializer: ICustomIngredientSerializer<*>)
	{
		val registry = DeferredRegister.create(NeoForgeRegistries.INGREDIENT_TYPES, serializer.identifier.namespace)
		registry.register(serializer.identifier.path) { _ -> serializer.neoforge }
		registry.register(MOD_BUS)
	}

	val <T : ICustomIngredient> ICustomIngredientSerializer<T>.neoforge: IngredientType<CustomIngredientPlatform.NeoForgeCustomIngredient<T>>
		get() = IngredientType(NeoForgeCustomIngredientCodec(this))

	class NeoForgeCustomIngredientCodec<A : ICustomIngredient>(
		custom: ICustomIngredientSerializer<A>
	) : MapCodec<CustomIngredientPlatform.NeoForgeCustomIngredient<A>>()
	{
		private val codec = custom.getCodec(false)

		override fun <T> keys(ops: DynamicOps<T>): Stream<T>
		{
			return codec.keys(ops)
		}

		override fun <T> encode(
			input: CustomIngredientPlatform.NeoForgeCustomIngredient<A>,
			ops: DynamicOps<T>,
			prefix: RecordBuilder<T>
		): RecordBuilder<T>
		{
			return codec.encode(input.custom, ops, prefix)
		}

		override fun <T> decode(
			ops: DynamicOps<T>,
			input: MapLike<T>
		): DataResult<CustomIngredientPlatform.NeoForgeCustomIngredient<A>>
		{
			return codec.decode(ops, input).map {
				it.neoforge
			}
		}
	}
}