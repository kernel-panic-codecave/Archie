package net.kernelpanicsoft.archie.data.common.crafting.ingredients

import com.mojang.serialization.MapCodec
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.crafting.Ingredient
import java.util.function.Function


/**
 * Base class for [IACustomIngredient]s that combine multiple sub-[ingredients], e.g. [AAllIngredient]
 * (matches when every sub-ingredient matches) and [AAnyIngredient] (matches when any does).
 */
abstract class ACombinedIngredient protected constructor(ingredients: List<Ingredient>) :
	IACustomIngredient
{
	/** The sub-ingredients being combined; always non-empty. */
	val ingredients: List<Ingredient>

	init
	{
		require(ingredients.isNotEmpty()) { "Combined ingredient must have at least one sub-ingredient" }

		this.ingredients = ingredients
	}

	/** `true` if any sub-ingredient is a custom ingredient that itself requires testing. */
	override val requiresTesting: Boolean
		get()
		{
			for (ingredient in ingredients)
			{
				if (ingredient is IACustomIngredientHolder<*> && ingredient.custom.requiresTesting)
				{
					return true
				}
			}

			return false
		}

	/** Generic [IACustomIngredientSerializer] for [ACombinedIngredient] subtypes, built from a [factory] and empty/non-empty codecs. */
	class Serializer<I : ACombinedIngredient>(
		override val identifier: ResourceLocation,
		private val factory: Function<List<Ingredient>, I>,
		private val allowEmptyCodec: MapCodec<I>,
		private val disallowEmptyCodec: MapCodec<I>
	) :
		IACustomIngredientSerializer<I>
	{
		override fun getCodec(allowEmpty: Boolean): MapCodec<I>
		{
			return if (allowEmpty) allowEmptyCodec else disallowEmptyCodec
		}

		override val packetCodec: StreamCodec<RegistryFriendlyByteBuf, I> = run {
			Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list())
				.map(factory, ACombinedIngredient::ingredients)
		}
	}

}
