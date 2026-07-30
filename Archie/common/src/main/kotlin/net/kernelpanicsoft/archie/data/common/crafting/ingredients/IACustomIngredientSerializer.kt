package net.kernelpanicsoft.archie.data.common.crafting.ingredients

import com.mojang.serialization.MapCodec
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.crafting.Ingredient


/**
 * Serializer for an [IACustomIngredient] of type [T].
 *
 * All instances must be registered using [register] for deserialization to work.
 */
interface IACustomIngredientSerializer<T : IACustomIngredient>
{
	/** The id this serializer is registered under; used to identify it in recipe JSON. */
	val identifier: ResourceLocation

	/**
	 * The codec used to read the ingredient from recipe JSON files.
	 *
	 * @param allowEmpty Whether an ingredient matching no items should be accepted, mirroring
	 * [Ingredient.CODEC] vs `Ingredient.CODEC_NONEMPTY`.
	 */
	fun getCodec(allowEmpty: Boolean): MapCodec<T>

	/** The codec used to sync the ingredient to the client over the network. */
	val packetCodec: StreamCodec<RegistryFriendlyByteBuf, T>

	companion object
	{
		/**
		 * Registers [serializer] under its [identifier][IACustomIngredientSerializer.identifier].
		 *
		 * @throws IllegalArgumentException if a serializer is already registered under that identifier
		 */
		fun <T : IACustomIngredient> register(serializer: IACustomIngredientSerializer<T>)
		{
			return ACustomIngredientSerializerPlatform.register(serializer)
		}
	}
}