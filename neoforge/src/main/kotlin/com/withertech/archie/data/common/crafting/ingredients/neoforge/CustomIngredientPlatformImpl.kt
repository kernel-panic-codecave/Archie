package com.withertech.archie.data.common.crafting.ingredients.neoforge

import com.withertech.archie.data.common.crafting.ingredients.ICustomIngredient as ArchieIngredient
import com.withertech.archie.data.common.crafting.ingredients.ICustomIngredientHolder
import com.withertech.archie.data.common.crafting.ingredients.neoforge.CustomIngredientSerializerPlatformImpl.neoforge
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.neoforged.neoforge.common.crafting.ICustomIngredient
import net.neoforged.neoforge.common.crafting.IngredientType
import net.neoforged.neoforge.registries.NeoForgeRegistries
import java.util.stream.Stream

object CustomIngredientPlatformImpl
{
	@JvmStatic
	fun vanillaOf(custom: ArchieIngredient): Ingredient
	{
		return custom.neoforge.toVanilla()
	}

	val <T : ArchieIngredient> T.neoforge: NeoForgeCustomIngredient<T>
		get() = NeoForgeCustomIngredient(this)

	class NeoForgeCustomIngredient<T : ArchieIngredient>(
		override val custom: T
	) : ICustomIngredient, ICustomIngredientHolder<T>
	{
		override fun test(arg: ItemStack): Boolean = custom.test(arg)

		override fun getItems(): Stream<ItemStack> = custom.matchingStacks.stream()

		override fun isSimple(): Boolean = false

		override fun getType(): IngredientType<*> = NeoForgeRegistries.INGREDIENT_TYPES[custom.serializer.identifier]!!
	}
}