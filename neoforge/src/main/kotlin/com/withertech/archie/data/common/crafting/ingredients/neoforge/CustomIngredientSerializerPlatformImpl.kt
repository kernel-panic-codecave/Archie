package com.withertech.archie.data.common.crafting.ingredients.neoforge

import com.mojang.datafixers.util.Pair
import com.mojang.serialization.*
import com.withertech.archie.data.common.crafting.ingredients.ICustomIngredient
import com.withertech.archie.data.common.crafting.ingredients.ICustomIngredientSerializer
import com.withertech.archie.data.common.crafting.ingredients.neoforge.CustomIngredientPlatformImpl.neoforge
import net.neoforged.neoforge.common.crafting.IngredientType
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import java.util.stream.Stream

object CustomIngredientSerializerPlatformImpl
{
	@JvmStatic
	fun register(serializer: ICustomIngredientSerializer<*>)
	{
		val registry = DeferredRegister.create(NeoForgeRegistries.INGREDIENT_TYPES, serializer.identifier.namespace)
		registry.register(serializer.identifier.path) { _ -> serializer.neoforge }
		registry.register(MOD_BUS)
	}

	val <T : ICustomIngredient> ICustomIngredientSerializer<T>.neoforge: IngredientType<CustomIngredientPlatformImpl.NeoForgeCustomIngredient<T>>
		get() = IngredientType(NeoForgeCustomIngredientCodec(this))

	class NeoForgeCustomIngredientCodec<A : ICustomIngredient>(
		custom: ICustomIngredientSerializer<A>
	) : MapCodec<CustomIngredientPlatformImpl.NeoForgeCustomIngredient<A>>()
	{
		private val codec = custom.getCodec(false)

		override fun <T> keys(ops: DynamicOps<T>): Stream<T>
		{
			return codec.keys(ops)
		}

		override fun <T> encode(
			input: CustomIngredientPlatformImpl.NeoForgeCustomIngredient<A>,
			ops: DynamicOps<T>,
			prefix: RecordBuilder<T>
		): RecordBuilder<T>
		{
			return codec.encode(input.custom, ops, prefix)
		}

		override fun <T> decode(
			ops: DynamicOps<T>,
			input: MapLike<T>
		): DataResult<CustomIngredientPlatformImpl.NeoForgeCustomIngredient<A>>
		{
			return codec.decode(ops, input).map {
				it.neoforge
			}
		}

	}
}