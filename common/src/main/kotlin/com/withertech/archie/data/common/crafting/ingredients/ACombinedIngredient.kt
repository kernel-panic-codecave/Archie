package com.withertech.archie.data.common.crafting.ingredients

import com.mojang.serialization.MapCodec
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.crafting.Ingredient
import java.util.function.Function


/**
 * Base class for ALL and ANY ingredients.
 */
abstract class ACombinedIngredient protected constructor(ingredients: List<Ingredient>) :
	IACustomIngredient
{
	val ingredients: List<Ingredient>

	init
	{
		require(ingredients.isNotEmpty()) { "Combined ingredient must have at least one sub-ingredient" }

		this.ingredients = ingredients
	}

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
