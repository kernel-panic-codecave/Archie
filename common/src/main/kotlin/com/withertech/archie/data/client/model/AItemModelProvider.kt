package com.withertech.archie.data.client.model

import dev.architectury.platform.Mod
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.PackOutput
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import java.util.*

abstract class AItemModelProvider(output: PackOutput, mod: Mod, exitOnError: Boolean) :
	AModelProvider<AItemModelBuilder>(output, mod, ITEM_FOLDER, ::AItemModelBuilder, exitOnError)
{
	fun basicItem(item: Item, block: AItemModelBuilder.() -> Unit = {}): AItemModelBuilder
	{
		return basicItem(Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(item)), block)
	}
	fun basicItem(item: ResourceLocation, block: AItemModelBuilder.() -> Unit = {}): AItemModelBuilder
	{
		return getBuilder(item.toString()) {
			parent(AModelFile("item/generated"))
			texture("layer0", ResourceLocation.fromNamespaceAndPath(item.namespace, "item/${item.path}"))
		}.apply(block)
	}

	override fun getName(): String = format("Item Models")
}