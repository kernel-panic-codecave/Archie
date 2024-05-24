package com.withertech.archie.data.common.crafting.ingredients

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.withertech.archie.Archie
import com.withertech.archie.serialization.buildCompoundTag
import net.benwoodworth.knbt.NbtCompoundBuilder
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.TagParser
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.crafting.Ingredient
import java.util.function.BiFunction
import java.util.function.Function
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class CustomDataIngredient private constructor(
	val base: Ingredient,

	nbt: CompoundTag
) : ICustomIngredient
{
	val nbt: CompoundTag

	init
	{
		require(!nbt.isEmpty) { "NBT cannot be null or empty; use components ingredient for strict matching" }
		this.nbt = nbt
	}

	override fun test(stack: ItemStack): Boolean
	{
		if (!base.test(stack)) return false

		val nbt: CustomData? = stack[DataComponents.CUSTOM_DATA]

		return nbt?.matchedBy(this.nbt) ?: false
	}

	override val matchingStacks: MutableList<ItemStack> by lazy {
		val stacks: MutableList<ItemStack> = base.items.toMutableList()
		stacks.replaceAll { stack ->
			val copy: ItemStack = stack.copy()
			copy.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY) {
				CustomData.of(it.copyTag().merge(this.nbt))
			}
			copy
		}

		stacks
	}

	override val requiresTesting: Boolean = true
	override val serializer: ICustomIngredientSerializer<*> = Serializer


	object Serializer : ICustomIngredientSerializer<CustomDataIngredient>
	{
		private val ID = Archie["custom_data"]

		private val ALLOW_EMPTY_CODEC: MapCodec<CustomDataIngredient> = createCodec(
			Ingredient.CODEC
		)
		private val DISALLOW_EMPTY_CODEC: MapCodec<CustomDataIngredient> = createCodec(
			Ingredient.CODEC_NONEMPTY
		)

		private val PACKET_CODEC: StreamCodec<RegistryFriendlyByteBuf, CustomDataIngredient> = StreamCodec.composite(
			Ingredient.CONTENTS_STREAM_CODEC,
			CustomDataIngredient::base,
			ByteBufCodecs.COMPOUND_TAG,
			CustomDataIngredient::nbt,
			::CustomDataIngredient
		)

		private fun createCodec(ingredientCodec: Codec<Ingredient>): MapCodec<CustomDataIngredient>
		{
			return RecordCodecBuilder.mapCodec { instance: RecordCodecBuilder.Instance<CustomDataIngredient> ->
				instance.group(
					ingredientCodec.fieldOf("base").forGetter(CustomDataIngredient::base),
					TagParser.LENIENT_CODEC.fieldOf("nbt").forGetter(CustomDataIngredient::nbt)
				).apply(
					instance, ::CustomDataIngredient
				)
			}
		}

		override val identifier: ResourceLocation = ID

		override fun getCodec(allowEmpty: Boolean): MapCodec<CustomDataIngredient> =
			if (allowEmpty) ALLOW_EMPTY_CODEC else DISALLOW_EMPTY_CODEC

		override val packetCodec: StreamCodec<RegistryFriendlyByteBuf, CustomDataIngredient> = PACKET_CODEC
	}

	companion object
	{
		fun of(base: Ingredient, nbt: CompoundTag): Ingredient = CustomDataIngredient(base, nbt).vanilla

		@OptIn(ExperimentalContracts::class)
		fun of(base: Ingredient, builderAction: NbtCompoundBuilder.() -> Unit)
		{
			contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
			of(base, buildCompoundTag(builderAction))
		}
	}

}
