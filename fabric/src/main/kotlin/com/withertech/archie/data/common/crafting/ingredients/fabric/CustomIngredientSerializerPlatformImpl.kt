package com.withertech.archie.data.common.crafting.ingredients.fabric

import com.mojang.datafixers.util.Pair
import com.mojang.serialization.*
import com.withertech.archie.data.common.crafting.ingredients.ICustomIngredient
import com.withertech.archie.data.common.crafting.ingredients.ICustomIngredientSerializer
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import java.util.stream.Stream

object CustomIngredientSerializerPlatformImpl
{
	@JvmStatic
	fun register(serializer: ICustomIngredientSerializer<*>)
	{
		CustomIngredientSerializer.register(serializer.fabric)
	}

	val <T : ICustomIngredient> ICustomIngredientSerializer<T>.fabric: CustomIngredientSerializer<CustomIngredientPlatformImpl.FabricCustomIngredient<T>>
		get() = FabricCustomIngredientSerializer(this)

	class FabricCustomIngredientSerializer<A : ICustomIngredient>(
		private val custom: ICustomIngredientSerializer<A>
	) : CustomIngredientSerializer<CustomIngredientPlatformImpl.FabricCustomIngredient<A>>
	{
		override fun getIdentifier(): ResourceLocation
		{
			return custom.identifier
		}

		override fun getCodec(allowEmpty: Boolean): MapCodec<CustomIngredientPlatformImpl.FabricCustomIngredient<A>>
		{
			val codec = custom.getCodec(allowEmpty)
			return object : MapCodec<CustomIngredientPlatformImpl.FabricCustomIngredient<A>>()
			{
				override fun <T> encode(
					input: CustomIngredientPlatformImpl.FabricCustomIngredient<A>,
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
				): DataResult<CustomIngredientPlatformImpl.FabricCustomIngredient<A>>
				{
					return codec.decode(ops, input).map {
						CustomIngredientPlatformImpl.FabricCustomIngredient(it)
					}
				}
			}
		}

		override fun getPacketCodec(): StreamCodec<RegistryFriendlyByteBuf, CustomIngredientPlatformImpl.FabricCustomIngredient<A>>
		{
			return custom.packetCodec.map({CustomIngredientPlatformImpl.FabricCustomIngredient(it)}, {it.custom})
		}
	}
}