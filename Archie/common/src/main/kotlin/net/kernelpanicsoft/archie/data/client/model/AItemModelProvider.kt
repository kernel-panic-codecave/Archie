package net.kernelpanicsoft.archie.data.client.model

import dev.architectury.platform.Mod
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.PackOutput
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import java.util.*

/** [AModelProvider] that generates item models under `models/item/`. */
abstract class AItemModelProvider(output: PackOutput, mod: Mod, exitOnError: Boolean) :
	AModelProvider<AItemModelBuilder>(output, mod, ITEM_FOLDER, ::AItemModelBuilder, exitOnError)
{
	/** Registers a `item/generated` model for [item] using its own `item/<path>` texture as `layer0`. */
	fun basicItem(item: Item, block: AItemModelBuilder.() -> Unit = {}): AItemModelBuilder
	{
		return basicItem(Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(item)), block)
	}

	/** Registers a `item/generated` model at [item] using an `item/<path>` texture as `layer0`. */
	fun basicItem(item: ResourceLocation, block: AItemModelBuilder.() -> Unit = {}): AItemModelBuilder
	{
		return getBuilder(item.toString()) {
			parent(AModelFile("item/generated"))
			texture("layer0", ResourceLocation.fromNamespaceAndPath(item.namespace, "item/${item.path}"))
		}.apply(block)
	}

	override fun getName(): String = format("Item Models")
}