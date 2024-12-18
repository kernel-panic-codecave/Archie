package com.withertech.archie.data.common.crafting.ingredients

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.withertech.archie.Archie
import com.withertech.archie.serialization.buildComponentPatch
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class AComponentsIngredient private constructor(val base: Ingredient, components: DataComponentPatch) : IACustomIngredient
{
	val components: DataComponentPatch

	init
	{
		require(!components.isEmpty) { "ComponentIngredient must have at least one defined component" }
		this.components = components
	}

	override fun test(stack: ItemStack): Boolean
	{
		if (!base.test(stack)) return false

		for ((type, value) in components.entrySet())
		{
			if (value.isPresent)
			{
				if (!stack.has(type)) return false

				if (value.get() != stack.get(type)) return false
			} else
			{
				if (stack.has(type)) return false
			}
		}

		return true
	}

	override val matchingStacks: MutableList<ItemStack> by lazy {
		val stacks: MutableList<ItemStack> = base.items.toMutableList()
		stacks.replaceAll { stack ->
			val copy = stack.copy()
			copy.applyComponentsAndValidate(components)
			copy
		}
		stacks
	}

	override val requiresTesting: Boolean = true
	override val serializer: IACustomIngredientSerializer<*> = Serializer

	object Serializer : IACustomIngredientSerializer<AComponentsIngredient>
	{
		private val ID = Archie["components"]

		private val ALLOW_EMPTY_CODEC: MapCodec<AComponentsIngredient> = createCodec(
			Ingredient.CODEC
		)
		private val DISALLOW_EMPTY_CODEC: MapCodec<AComponentsIngredient> = createCodec(
			Ingredient.CODEC_NONEMPTY
		)

		private val PACKET_CODEC: StreamCodec<RegistryFriendlyByteBuf, AComponentsIngredient> = StreamCodec.composite(
			Ingredient.CONTENTS_STREAM_CODEC,
			AComponentsIngredient::base,
			DataComponentPatch.STREAM_CODEC,
			AComponentsIngredient::components,
			::AComponentsIngredient
		)

		private fun createCodec(ingredientCodec: Codec<Ingredient>): MapCodec<AComponentsIngredient>
		{
			return RecordCodecBuilder.mapCodec { instance: RecordCodecBuilder.Instance<AComponentsIngredient> ->
				instance.group(
					ingredientCodec.fieldOf("base").forGetter(AComponentsIngredient::base),
					DataComponentPatch.CODEC.fieldOf("components").forGetter(AComponentsIngredient::components)
				).apply(
					instance, ::AComponentsIngredient
				)
			}
		}

		override val identifier: ResourceLocation = ID

		override fun getCodec(allowEmpty: Boolean): MapCodec<AComponentsIngredient> =
			if (allowEmpty) ALLOW_EMPTY_CODEC else DISALLOW_EMPTY_CODEC

		override val packetCodec: StreamCodec<RegistryFriendlyByteBuf, AComponentsIngredient> = PACKET_CODEC
	}

	companion object
	{
		fun of(base: Ingredient, components: DataComponentPatch): Ingredient = AComponentsIngredient(base, components).vanilla
		@OptIn(ExperimentalContracts::class)
		fun of(base: Ingredient, builderAction: DataComponentPatch.Builder.() -> Unit): Ingredient
		{
			contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
			return of(base, buildComponentPatch(builderAction))
		}
	}
}