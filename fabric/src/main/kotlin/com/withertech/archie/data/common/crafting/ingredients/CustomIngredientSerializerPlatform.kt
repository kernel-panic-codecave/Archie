package com.withertech.archie.data.common.crafting.ingredients

import com.mojang.serialization.*
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import java.util.stream.Stream

actual object CustomIngredientSerializerPlatform
{
	actual fun register(serializer: ICustomIngredientSerializer<*>)
	{
		CustomIngredientSerializer.register(serializer.fabric)
	}

	val <T : ICustomIngredient> ICustomIngredientSerializer<T>.fabric: CustomIngredientSerializer<CustomIngredientPlatform.FabricCustomIngredient<T>>
		get() = FabricCustomIngredientSerializer(this)

	class FabricCustomIngredientSerializer<A : ICustomIngredient>(
		private val custom: ICustomIngredientSerializer<A>
	) : CustomIngredientSerializer<CustomIngredientPlatform.FabricCustomIngredient<A>>
	{
		override fun getIdentifier(): ResourceLocation
		{
			return custom.identifier
		}

		override fun getCodec(allowEmpty: Boolean): MapCodec<CustomIngredientPlatform.FabricCustomIngredient<A>>
		{
			val codec = custom.getCodec(allowEmpty)
			return object : MapCodec<CustomIngredientPlatform.FabricCustomIngredient<A>>()
			{
				override fun <T> encode(
					input: CustomIngredientPlatform.FabricCustomIngredient<A>,
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
				): DataResult<CustomIngredientPlatform.FabricCustomIngredient<A>>
				{
					return codec.decode(ops, input).map {
						CustomIngredientPlatform.FabricCustomIngredient(it)
					}
				}
			}
		}

		override fun getPacketCodec(): StreamCodec<RegistryFriendlyByteBuf, CustomIngredientPlatform.FabricCustomIngredient<A>>
		{
			return custom.packetCodec.map({ CustomIngredientPlatform.FabricCustomIngredient(it)}, {it.custom})
		}
	}
}