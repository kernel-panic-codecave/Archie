package com.withertech.archie.gui.elements

import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.DataSlot
import net.minecraft.world.inventory.Slot

interface IContainerElement
{
	val slots: List<Slot> get() = listOf()
	val ints: List<DataSlot> get() = listOf()
	val intArrays: List<ContainerData> get() = listOf()
}