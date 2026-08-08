package net.kernelpanicsoft.archie.transfer

import earth.terrarium.common_storage_lib.item.impl.vanilla.AbstractVanillaContainer
import earth.terrarium.common_storage_lib.item.impl.vanilla.VanillaDelegatingSlot
import earth.terrarium.common_storage_lib.storage.base.UpdateManager
import net.kernelpanicsoft.archie.gui.ComposeContainerMenuBase
import net.minecraft.world.SimpleContainer
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import java.util.function.Predicate

/**
 * The [ArchieItemMenuSlot] equivalent for adapting an existing vanilla-style
 * [AbstractVanillaContainer] (rather than an [ArchieItemStorage]) into a [ComposeContainerMenuBase].
 * Created by [ComposeContainerMenuBase] from a `handler(group, storage, filter)` registration; not
 * usually constructed directly.
 *
 * @param filter Restricts which stacks [mayPlace] into this slot.
 */
class VanillaMenuSlot(
	private val storage: AbstractVanillaContainer,
	val filter: Predicate<ItemStack> = Predicate { true },
	slot: Int, x: Int, y: Int,
	private val owningMenu: ComposeContainerMenuBase<*>,
) : Slot(SimpleContainer(0), slot, x, y)
{
	override fun isActive(): Boolean = owningMenu.isSlotVisible(index)


	override fun getItem(): ItemStack
	{
		val slot = storage[containerSlot] as VanillaDelegatingSlot
		return slot.createSnapshot()
	}

	override fun set(stack: ItemStack)
	{
		val slot = storage[containerSlot] as VanillaDelegatingSlot
		slot.readSnapshot(stack)
		setChanged()
	}

	override fun getMaxStackSize(): Int
	{
		val slot = storage[containerSlot] as VanillaDelegatingSlot
		return slot.getLimit(slot.resource).toInt()
	}

	override fun setChanged()
	{
		UpdateManager.batch(storage)
	}

	override fun remove(amount: Int): ItemStack
	{
		// set(it) already calls setChanged() - an unconditional call here would double up the
		// UpdateManager.batch() dispatch, and would also fire when nothing was actually removed.
		return if (!item.isEmpty && amount > 0) item.let {
			val ret = it.split(amount)
			set(it)
			ret
		} else ItemStack.EMPTY
	}

	override fun mayPlace(stack: ItemStack): Boolean
	{
		return filter.test(stack)
	}
}