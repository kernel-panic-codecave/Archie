package net.kernelpanicsoft.archie.transfer

import earth.terrarium.common_storage_lib.storage.base.UpdateManager
import net.minecraft.world.SimpleContainer
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import java.util.function.Predicate

class ArchieItemMenuSlot(private val storage: ArchieItemStorage, val filter: Predicate<ItemStack> = Predicate { true }, slot: Int, x: Int, y: Int) : Slot(SimpleContainer(0), slot, x, y)
{
	override fun getItem(): ItemStack
	{
		val slot = storage[containerSlot]
		return slot.getItem()
	}

	override fun set(stack: ItemStack)
	{
		val slot = storage[containerSlot]
		slot.set(stack)
		setChanged()
	}

	override fun getMaxStackSize(): Int
	{
		val slot = storage[containerSlot]
		return slot.getMaxStackSize()
	}

	override fun setChanged()
	{
		UpdateManager.batch(storage)
	}

	override fun remove(amount: Int): ItemStack
	{
		val slot = storage[containerSlot]
		val ret = slot.remove(amount)
		setChanged()
		return ret
	}

	override fun mayPlace(stack: ItemStack): Boolean
	{
		return filter.test(stack)
	}
}