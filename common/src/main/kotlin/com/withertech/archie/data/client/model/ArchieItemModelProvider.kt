package com.withertech.archie.data.client.model

import dev.architectury.platform.Mod
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.PackOutput
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import java.util.*

abstract class ArchieItemModelProvider(output: PackOutput, mod: Mod, exitOnError: Boolean) :
	ArchieModelProvider<ItemModelBuilder>(output, mod, ITEM_FOLDER, ::ItemModelBuilder, exitOnError)
{
	fun basicItem(item: Item, block: ItemModelBuilder.() -> Unit = {}): ItemModelBuilder
	{
		return basicItem(Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(item)), block)
	}
	fun basicItem(item: ResourceLocation, block: ItemModelBuilder.() -> Unit = {}): ItemModelBuilder
	{
		return getBuilder(item.toString()) {
			parent(ModelFile("item/generated"))
			texture("layer0", ResourceLocation(item.namespace, "item/${item.path}"))
		}.apply(block)
	}

	override fun getName(): String = format("Item Models")
}