package net.kernelpanicsoft.archie.config.builder

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
import net.kernelpanicsoft.archie.config.builder.ofBlockEntityTypeObject
import java.lang.reflect.Field
import kotlin.reflect.KClass

/**
 * Cloth Config builder for a single [registry] entry, rendered as a dropdown over every entry
 * (optionally filtered to instances of [subclass]) sorted by registry name. Recognized entry
 * types (`Item`, `Block`, `BlockEntityType`) get an icon in their dropdown cell; anything else
 * falls back to a plain text cell.
 */
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