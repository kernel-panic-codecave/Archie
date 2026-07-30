package net.kernelpanicsoft.archie.data.common.crafting.ingredients

import net.kernelpanicsoft.archie.data.common.crafting.ingredients.IACustomIngredient as ArchieIngredient
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.neoforged.neoforge.common.crafting.ICustomIngredient
import net.neoforged.neoforge.common.crafting.IngredientType
import net.neoforged.neoforge.registries.NeoForgeRegistries
import java.util.stream.Stream

/** NeoForge implementation of [ACustomIngredientPlatform], adapting [ArchieIngredient] onto NeoForge's `ICustomIngredient` API. */
actual object ACustomIngredientPlatform
{
	/** Wraps [custom] as a NeoForge [ICustomIngredient] and converts it to a vanilla [Ingredient]. */
	actual fun vanillaOf(custom: ArchieIngredient): Ingredient
	{
		return custom.neoforge.toVanilla()
	}

	/** Wraps this ingredient as a NeoForge [ICustomIngredient]. */
	val <T : ArchieIngredient> T.neoforge: NeoForgeCustomIngredient<T>
		get() = NeoForgeCustomIngredient(this)

	/** Adapts an [ArchieIngredient] to NeoForge's [ICustomIngredient] interface, delegating all matching logic to it; never treated as [isSimple]. */
	class NeoForgeCustomIngredient<T : ArchieIngredient>(
		override val custom: T
	) : ICustomIngredient, IACustomIngredientHolder<T>
	{
		override fun test(arg: ItemStack): Boolean = custom.test(arg)

		override fun getItems(): Stream<ItemStack> = custom.matchingStacks.stream()

		override fun isSimple(): Boolean = false

		override fun getType(): IngredientType<*> = NeoForgeRegistries.INGREDIENT_TYPES[custom.serializer.identifier]!!
	}
}