package com.withertech.archie.data.common.crafting.ingredients

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.crafting.Ingredient


/**
 * Serializer for a [ICustomIngredient].
 *
 *
 * All instances must be registered using [.register] for deserialization to work.
 *
 * @param <T> the type of the custom ingredient
</T> */
interface ICustomIngredientSerializer<T : ICustomIngredient>
{
	/**
	 * {@return the identifier of this serializer}.
	 */
	val identifier: ResourceLocation

	/**
	 * {@return the codec}.
	 *
	 *
	 * Codecs are used to read the ingredient from the recipe JSON files.
	 *
	 * @see Ingredient.CODEC
	 *
	 * @see Ingredient.CODEC_NONEMPTY
	 */
	fun getCodec(allowEmpty: Boolean): MapCodec<T>

	val packetCodec: StreamCodec<RegistryFriendlyByteBuf, T>

	companion object
	{
		/**
		 * Registers a custom ingredient serializer, using the [serializer&#39;s identifier][CustomIngredientSerializer.getIdentifier].
		 *
		 * @throws IllegalArgumentException if the serializer is already registered
		 */
		fun register(serializer: ICustomIngredientSerializer<*>)
		{
			return CustomIngredientSerializerPlatform.register(serializer)
		}
	}
}