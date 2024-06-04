package com.withertech.archie.gui.container

import com.withertech.archie.transfer.ArchieItemMenuSlot
import com.withertech.archie.transfer.ArchieItemStorage
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity

abstract class ArchieContainerMenu<T : BlockEntity, SELF : ArchieContainerMenu<T, SELF>>(
	type: MenuType<SELF>,
	id: Int,
	protected val inventory: Inventory,
	protected val tile: T
) : AbstractContainerMenu(type, id)
{
	protected abstract val playerXOffset: Int
	protected abstract val playerYOffset: Int

	private val _slotPositions: MutableList<Pair<Int, Int>> = mutableListOf()
	val slotPositions: List<Pair<Int, Int>>
		get() = _slotPositions

	protected val player: Player = inventory.player
	protected val level: Level = player.level()

	private var menuSize: Int = 0
	protected open val menuSlots: IntRange get() = 0 until menuSize
	protected open val playerSlots: IntRange get() = menuSize until slots.size


	protected fun addSlots()
	{
		slots.clear()
		addMenuSlots()
		menuSize = slots.size
		addPlayerSlots()
	}

	protected abstract fun addMenuSlots()

	data class SlotGridLocation(val slot: Int, val x: Int, val y: Int)

	protected fun slotGrid(x: Int, y: Int, width: Int, height: Int, block: SlotGridLocation.() -> Unit)
	{
		for (i in 0 until height)
		{
			for (j in 0 until width)
			{
				SlotGridLocation(j + i * width, x + j * 18, y + i * 18).block()
			}
		}
	}

	protected fun slotGrid(x: Int, y: Int, width: Int, height: Int, container: Container)
	{
		slotGrid(x, y, width, height) { slot(container, this.slot, this.x, this.y) }
	}

	protected fun slotGrid(x: Int, y: Int, width: Int, height: Int, storage: ArchieItemStorage)
	{
		slotGrid(x, y, width, height) { slot(storage, this.slot, this.x, this.y) }
	}


	protected fun slot(slot: Slot)
	{
		_slotPositions.add(slot.x to slot.y)
		addSlot(slot)
	}

	protected fun slot(container: Container, slot: Int, x: Int, y: Int)
	{
		slot(Slot(container, slot, x, y))
	}

	protected fun slot(storage: ArchieItemStorage, slot: Int, x: Int, y: Int)
	{
		slot(ArchieItemMenuSlot(storage, slot, x, y))
	}

	private fun addPlayerSlots()
	{
		slotGrid(playerXOffset, playerYOffset, 9, 3) {
			slot(inventory, slot + 9, x, y)
		}

		slotGrid(playerXOffset, playerYOffset + 58, 9, 1, inventory)
	}

	override fun quickMoveStack(player: Player, index: Int): ItemStack
	{
		var stack = ItemStack.EMPTY
		val slot = slots[index]
		if (slot.hasItem())
		{
			val slotStack = slot.item
			stack = slotStack.copy()
			if (index in menuSlots)
			{
				if (!moveItemStackTo(slotStack, playerSlots.first, playerSlots.last, true))
					return ItemStack.EMPTY
			} else if (index in playerSlots)
			{
				if (!moveItemStackTo(slotStack, menuSlots.first, menuSlots.last, false))
					return ItemStack.EMPTY
			}

			if (slotStack.isEmpty)
			{
				slot.set(ItemStack.EMPTY)
			} else
			{
				slot.setChanged()
			}
		}
		return stack
	}

	override fun clicked(slotId: Int, button: Int, clickType: ClickType, player: Player)
	{
		super.clicked(slotId, button, clickType, player)
		broadcastFullState()
	}

	override fun stillValid(player: Player): Boolean
	{
		return true
	}
}