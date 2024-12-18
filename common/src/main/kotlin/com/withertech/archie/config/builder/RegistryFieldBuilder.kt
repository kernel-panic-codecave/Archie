package com.withertech.archie.config.builder

import me.shedaniel.clothconfig2.gui.entries.DropdownBoxEntry
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.Registry
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import com.withertech.archie.config.builder.ofBlockEntityTypeObject
import java.lang.reflect.Field
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
class RegistryFieldBuilder<T : Any, R : T>(
	resetButtonKey: Component,
	fieldNameKey: Component,
	subclass: KClass<R>? = null,
	registry: Registry<T>,
	value: T
) : DropdownMenuBuilder<T>(resetButtonKey, fieldNameKey, TopCellElementBuilder.of(value, {
	registry.getOptional(
		ResourceLocation.parse(it)
	).orElse(null)
}, {
	Component.literal(registry.getKey(it).toString())
}), when (value)
{
	is Item -> CellCreatorBuilder.ofItemObject() as DropdownBoxEntry.SelectionCellCreator<T>
	is Block -> CellCreatorBuilder.ofBlockObject() as DropdownBoxEntry.SelectionCellCreator<T>
	is BlockEntityType<*> -> ofBlockEntityTypeObject() as DropdownBoxEntry.SelectionCellCreator<T>
	else -> CellCreatorBuilder.of(20, 146, 7) {
		Component.literal(registry.getKey(it).toString())
	}
})
{
	init
	{
		selections = (
				if (subclass != null) registry.filterIsInstance(subclass.java)
				else registry
				).sortedBy {
				registry.getKey(it).toString()
			}.toSet()
	}
}