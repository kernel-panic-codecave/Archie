package net.kernelpanicsoft.archie.util

import dev.architectury.extensions.injected.InjectedItemPropertiesExtension
import dev.architectury.registry.registries.DeferredSupplier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.state.BlockBehaviour

/**
 * Builds a [BlockBehaviour.Properties] via [block], optionally starting from a full copy of
 * [parent]'s properties instead of the defaults.
 */
fun blockProperties(parent: BlockBehaviour? = null, block: BlockBehaviour.Properties.() -> Unit): BlockBehaviour.Properties
{
	return (parent?.let { BlockBehaviour.Properties.ofFullCopy(it) } ?: BlockBehaviour.Properties.of()).apply(block)
}

/** Builds an [Item.Properties] via [block]. */
fun itemProperties(block: Item.Properties.() -> Unit): Item.Properties
{
	return Item.Properties().apply(block)
}

/** Assigns [tab] as this item's creative tab, via Architectury's injected item-properties extension. */
@Suppress("UnstableApiUsage")
fun Item.Properties.tab(tab: CreativeModeTab): Item.Properties = (this as InjectedItemPropertiesExtension).`arch$tab`(tab)

/** Assigns [tab] as this item's creative tab, via Architectury's injected item-properties extension. */
@Suppress("UnstableApiUsage")
fun Item.Properties.tab(tab: DeferredSupplier<CreativeModeTab>): Item.Properties = (this as InjectedItemPropertiesExtension).`arch$tab`(tab)

/** Assigns [tab] as this item's creative tab, via Architectury's injected item-properties extension. */
@Suppress("UnstableApiUsage")
fun Item.Properties.tab(tab: ResourceKey<CreativeModeTab>): Item.Properties = (this as InjectedItemPropertiesExtension).`arch$tab`(tab)