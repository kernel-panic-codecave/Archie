package net.kernelpanicsoft.archie.transfer

import earth.terrarium.common_storage_lib.resources.item.ItemResource
import earth.terrarium.common_storage_lib.storage.base.CommonStorage
import earth.terrarium.common_storage_lib.storage.base.StorageSlot
import earth.terrarium.common_storage_lib.storage.base.UpdateManager
import net.kernelpanicsoft.archie.gui.ComposeContainerMenuBase
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import java.util.function.Predicate

/**
 * A vanilla [Slot] over one slot of *any* [CommonStorage] of items, so `net.minecraft.world.inventory`
 * machinery (shift-click, drag, hotbar swap, ...) can operate on a storage that is not an
 * [ArchieItemStorage].
 *
 * [ArchieItemMenuSlot] is the specialisation for Archie's own storage, which can be read and written
 * as an [ItemStack] directly. This one has only the [StorageSlot] contract to work with, so it
 * phrases the same operations as extract/insert - which is exactly what lets a *view* over some
 * larger structure (one kind's layer of a multi-kind storage, a filtered projection, a storage
 * assembled from several others) appear in a menu at all. Without it such a storage silently
 * contributes no slots, since [ComposeContainerMenuBase] can only build from what it recognises.
 *
 * @param filter Restricts which stacks [mayPlace] into this slot.
 */
class CommonStorageMenuSlot(
	private val storage: CommonStorage<ItemResource>,
	val filter: Predicate<ItemStack> = Predicate { true },
	slot: Int, x: Int, y: Int,
	private val owningMenu: ComposeContainerMenuBase<*>,
) : Slot(SimpleContainer(0), slot, x, y)
{
	private val backing: StorageSlot<ItemResource> get() = storage[containerSlot]

	override fun isActive(): Boolean = owningMenu.isSlotVisible(index)

	override fun getItem(): ItemStack
	{
		val slot = backing
		val resource = slot.resource
		if (resource.isBlank || slot.amount <= 0) return ItemStack.EMPTY
		return resource.toStack(slot.amount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
	}

	/**
	 * Replaces this slot's contents.
	 *
	 * Phrased as "take out whatever is there, put this in", because a [StorageSlot] has no
	 * overwrite of its own - and that is the right meaning anyway: a storage that refuses part of
	 * [stack] keeps only what it accepted rather than being forced past its own limit.
	 */
	override fun set(stack: ItemStack)
	{
		val slot = backing
		val held = slot.resource
		if (!held.isBlank && slot.amount > 0) slot.extract(held, slot.amount, false)
		if (!stack.isEmpty) slot.insert(ItemResource.of(stack), stack.count.toLong(), false)
		setChanged()
	}

	override fun getMaxStackSize(): Int = backing.getLimit(backing.resource).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

	override fun getMaxStackSize(stack: ItemStack): Int =
		backing.getLimit(ItemResource.of(stack)).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

	/** Only a storage that manages its own updates has anything to batch here; anything else persists through its own insert/extract. */
	override fun setChanged()
	{
		(storage as? UpdateManager<*>)?.let { UpdateManager.batch(it) }
	}

	override fun remove(amount: Int): ItemStack
	{
		val slot = backing
		val resource = slot.resource
		if (resource.isBlank || amount <= 0) return ItemStack.EMPTY
		val taken = slot.extract(resource, amount.toLong(), false)
		if (taken <= 0) return ItemStack.EMPTY
		setChanged()
		return resource.toStack(taken.toInt())
	}

	override fun mayPlace(stack: ItemStack): Boolean =
		filter.test(stack) && backing.isResourceValid(ItemResource.of(stack))

	/** See [ArchieItemMenuSlot.allowModification] - the same reasoning, for the same reason. */
	override fun allowModification(player: Player): Boolean = mayPickup(player)
}
