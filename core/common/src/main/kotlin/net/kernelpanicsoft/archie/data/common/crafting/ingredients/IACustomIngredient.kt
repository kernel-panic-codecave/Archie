package net.kernelpanicsoft.archie.data.common.crafting.ingredients

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import org.jetbrains.annotations.ApiStatus

/**
 * Interface that modders can implement to create new recipe-matching behaviors beyond vanilla
 * [Ingredient]s, ported from Fabric's custom ingredient API to work cross-loader.
 *
 * This is not directly implemented on vanilla [Ingredient]s; use [vanilla] to convert a custom
 * ingredient into one. On disk, a custom ingredient is encoded by its [serializer], keyed by
 * that serializer's registered identifier, plus whatever extra fields the serializer needs.
 *
 * @see IACustomIngredientSerializer
 */
interface IACustomIngredient
{
	/**
	 * Checks whether [stack] matches this ingredient. Must not modify [stack].
	 */
	fun test(stack: ItemStack): Boolean

	/**
	 * The stacks that match this ingredient, for display purposes (e.g. in a recipe viewer).
	 *
	 * Guidelines for good compatibility:
	 * - These stacks need not be exhaustive or perfectly accurate, except when [requiresTesting]
	 *   is `false`, in which case they must correspond exactly to every accepted item.
	 * - At least one stack must be returned, or the ingredient is considered
	 *   [empty][Ingredient.isEmpty].
	 * - Try to include at least one stack per accepted item, so inspecting mods can enumerate
	 *   what the ingredient might accept.
	 *
	 * No caching is required here; the ingredient itself already caches this.
	 */
	val matchingStacks: MutableList<ItemStack>

	/**
	 * Whether [test] must always be called to know if a stack matches, as opposed to relying on
	 * [matchingStacks] alone. `false` when this ingredient ignores extra stack data (like
	 * components/NBT) and matching is fully determined by item type.
	 */
	val requiresTesting: Boolean

	/**
	 * The serializer for this ingredient. Must have been registered via
	 * [IACustomIngredientSerializer.register].
	 */
	val serializer: IACustomIngredientSerializer<*>

	/** Converts this custom ingredient into a vanilla [Ingredient] behaving the same way. */
	@get:ApiStatus.NonExtendable
	val vanilla: Ingredient
		get()
		{
			return ACustomIngredientPlatform.vanillaOf(this)
		}
}
