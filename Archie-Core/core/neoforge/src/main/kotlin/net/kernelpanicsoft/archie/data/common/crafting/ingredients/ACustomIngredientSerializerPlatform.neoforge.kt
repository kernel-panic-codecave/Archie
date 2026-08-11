package net.kernelpanicsoft.archie.data.common.crafting.ingredients

import com.mojang.serialization.*
import dev.nyon.klf.MOD_BUS
import net.kernelpanicsoft.archie.data.common.crafting.ingredients.ACustomIngredientPlatform.neoforge
import net.neoforged.neoforge.common.crafting.IngredientType
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import java.util.stream.Stream

/** NeoForge implementation of [ACustomIngredientSerializerPlatform], adapting [IACustomIngredientSerializer] onto NeoForge's `IngredientType` registry. */
actual object ACustomIngredientSerializerPlatform
{
	/** Registers [serializer] as a NeoForge [IngredientType] via [neoforge]. */
	actual fun <T : IACustomIngredient> register(serializer: IACustomIngredientSerializer<T>)
	{
		val registry = DeferredRegister.create(NeoForgeRegistries.INGREDIENT_TYPES, serializer.identifier.namespace)
		registry.register(serializer.identifier.path) { _ -> serializer.neoforge }
		registry.register(MOD_BUS)
	}

	/** Wraps this serializer as a NeoForge [IngredientType], backed by [NeoForgeCustomIngredientCodec]. */
	val <T : IACustomIngredient> IACustomIngredientSerializer<T>.neoforge: IngredientType<ACustomIngredientPlatform.NeoForgeCustomIngredient<T>>
		get() = IngredientType(NeoForgeCustomIngredientCodec(this))

	/** Adapts an [IACustomIngredientSerializer]'s codec to one producing/consuming [ACustomIngredientPlatform.NeoForgeCustomIngredient] wrappers; no packet codec since NeoForge derives sync from the data codec. */
	class NeoForgeCustomIngredientCodec<A : IACustomIngredient>(
		custom: IACustomIngredientSerializer<A>
	) : MapCodec<ACustomIngredientPlatform.NeoForgeCustomIngredient<A>>()
	{
		private val codec = custom.getCodec(false)

		override fun <T> keys(ops: DynamicOps<T>): Stream<T>
		{
			return codec.keys(ops)
		}

		override fun <T> encode(
			input: ACustomIngredientPlatform.NeoForgeCustomIngredient<A>,
			ops: DynamicOps<T>,
			prefix: RecordBuilder<T>
		): RecordBuilder<T>
		{
			return codec.encode(input.custom, ops, prefix)
		}

		override fun <T> decode(
			ops: DynamicOps<T>,
			input: MapLike<T>
		): DataResult<ACustomIngredientPlatform.NeoForgeCustomIngredient<A>>
		{
			return codec.decode(ops, input).map {
				it.neoforge
			}
		}
	}
}