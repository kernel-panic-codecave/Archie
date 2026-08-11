package net.kernelpanicsoft.archie.gui.item

import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

/**
 * Locates the [ItemStack] backing a [ComposeItemContainerMenu] and reports whether it's still
 * valid to keep the menu open.
 */
interface ItemContainerAccess
{
	/**
	 * Resolves the current backing [ItemStack]. Must be re-resolved fresh on every call, not
	 * cached - the underlying stack reference can be swapped out from under the menu (e.g. by
	 * another mod replacing the inventory slot's stack wholesale), and a cached reference would
	 * silently go stale rather than reflect that.
	 */
	fun getStack(): ItemStack

	/** Whether [player] should still be allowed to keep this menu open. */
	fun stillValid(player: Player): Boolean
}

/**
 * An [ItemContainerAccess] for an item sitting in [player]'s own inventory at [slot] (vanilla
 * [net.minecraft.world.entity.player.Inventory] numbering: hotbar 0-8, main 9-35).
 *
 * @param expectedItem Guards [stillValid] against the slot's contents having been swapped out
 *   for a different item entirely (e.g. dropped and something else picked up into the same
 *   slot index) while the menu was open.
 */
class PlayerInventoryItemAccess(
	private val player: Player,
	val slot: Int,
	private val expectedItem: Item,
) : ItemContainerAccess
{
	override fun getStack(): ItemStack = player.inventory.getItem(slot)

	override fun stillValid(player: Player): Boolean =
		player === this.player && getStack().`is`(expectedItem)
}
