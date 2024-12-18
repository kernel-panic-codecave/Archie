package com.withertech.archie.data.common.crafting.ingredients

import com.mojang.serialization.*
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import java.util.stream.Stream

actual object ACustomIngredientSerializerPlatform
{
	actual fun register(serializer: IACustomIngredientSerializer<*>)
	{
		CustomIngredientSerializer.register(serializer.fabric)
	}

	val <T : IACustomIngredient> IACustomIngredientSerializer<T>.fabric: CustomIngredientSerializer<ACustomIngredientPlatform.FabricCustomIngredient<T>>
		get() = FabricCustomIngredientSerializer(this)

	class FabricCustomIngredientSerializer<A : IACustomIngredient>(
		private val custom: IACustomIngredientSerializer<A>
	) : CustomIngredientSerializer<ACustomIngredientPlatform.FabricCustomIngredient<A>>
	{
		override fun getIdentifier(): ResourceLocation
		{
			return custom.identifier
		}

		override fun getCodec(allowEmpty: Boolean): MapCodec<ACustomIngredientPlatform.FabricCustomIngredient<A>>
		{
			val codec = custom.getCodec(allowEmpty)
			return object : MapCodec<ACustomIngredientPlatform.FabricCustomIngredient<A>>()
			{
				override fun <T> encode(
					input: ACustomIngredientPlatform.FabricCustomIngredient<A>,
					ops: DynamicOps<T>,
					prefix: RecordBuilder<T>
				): RecordBuilder<T>
				{
					return codec.encode(input.custom, ops, prefix)
				}

				override fun <T> keys(ops: DynamicOps<T>): Stream<T>
				{
					return codec.keys(ops)
				}

				override fun <T> decode(
					ops: DynamicOps<T>,
					input: MapLike<T>
				): DataResult<ACustomIngredientPlatform.FabricCustomIngredient<A>>
				{
					return codec.decode(ops, input).map {
						ACustomIngredientPlatform.FabricCustomIngredient(it)
					}
				}
			}
		}

		override fun getPacketCodec(): StreamCodec<RegistryFriendlyByteBuf, ACustomIngredientPlatform.FabricCustomIngredient<A>>
		{
			return custom.packetCodec.map({ ACustomIngredientPlatform.FabricCustomIngredient(it)}, {it.custom})
		}
	}
}