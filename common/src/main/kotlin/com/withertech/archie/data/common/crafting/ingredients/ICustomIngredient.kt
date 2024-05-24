package com.withertech.archie.data.common.crafting.ingredients

import dev.architectury.injectables.annotations.ExpectPlatform
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import org.jetbrains.annotations.ApiStatus

/**
 * Interface that modders can implement to create new behaviors for [Ingredient]s.
 *
 *
 * This is not directly implemented on vanilla [Ingredient]s, but conversions are possible:
 *
 *  * [.toVanilla] converts a custom ingredient to a vanilla [Ingredient].
 *  * [FabricIngredient] can be used to check if a vanilla [Ingredient] is custom,
 * and retrieve the custom ingredient in that case.
 *
 *
 *
 * The format for custom ingredients is as follows:
 * <pre>`{
 * "fabric:type": "<identifier of the serializer>",
 * // extra ingredient data, dependent on the serializer
 * }
`</pre> *
 *
 * @see ICustomIngredientSerializer
 */
interface ICustomIngredient
{
	/**
	 * Checks if a stack matches this ingredient.
	 * The stack **must not** be modified in any way.
	 *
	 * @param stack the stack to test
	 * @return `true` if the stack matches this ingredient, `false` otherwise
	 */
	fun test(stack: ItemStack): Boolean

	/**
	 * @return the list of stacks that match this ingredient.
	 *
	 *
	 * The following guidelines should be followed for good compatibility:
	 *
	 *  * These stacks are generally used for display purposes, and need not be exhaustive or perfectly accurate.
	 *  * An exception is ingredients that [don&#39;t require testing][.requiresTesting],
	 * for which it is important that the returned stacks correspond exactly to all the accepted [Item]s.
	 *  * At least one stack must be returned for the ingredient not to be considered [empty][Ingredient.isEmpty].
	 *  * The ingredient should try to return at least one stack with each accepted [Item].
	 * This allows mods that inspect the ingredient to figure out which stacks it might accept.
	 *
	 *
	 *
	 * Note: no caching needs to be done by the implementation, this is already handled by the ingredient itself.
	 */
	val matchingStacks: MutableList<ItemStack>

	/**
	 * Returns whether this ingredient always requires [direct stack testing][.test].
	 *
	 * @return `false` if this ingredient ignores NBT data when matching stacks, `true` otherwise
	 * @see FabricIngredient.requiresTesting
	 */
	val requiresTesting: Boolean

	/**
	 * @return The serializer for this ingredient
	 *
	 *
	 * The serializer must have been registered using [ICustomIngredientSerializer.register].
	 */
	val serializer: ICustomIngredientSerializer<*>

	/**
	 * @return a new [Ingredient] behaving as defined by this custom ingredient.
	 */
	@get:ApiStatus.NonExtendable
	val vanilla: Ingredient
		get()
		{
			return CustomIngredientPlatform.vanillaOf(this)
		}
}
