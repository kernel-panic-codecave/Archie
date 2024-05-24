package com.withertech.archie.util

import dev.architectury.registry.registries.DeferredSupplier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.state.BlockBehaviour

fun blockProperties(parent: BlockBehaviour? = null, block: BlockBehaviour.Properties.() -> Unit): BlockBehaviour.Properties
{
	return (parent?.let { BlockBehaviour.Properties.ofFullCopy(it) } ?: BlockBehaviour.Properties.of()).apply(block)
}

fun itemProperties(block: Item.Properties.() -> Unit): Item.Properties
{
	return Item.Properties().apply(block)
}

@Suppress("UnstableApiUsage")
fun Item.Properties.tab(tab: CreativeModeTab): Item.Properties = `arch$tab`(tab)

@Suppress("UnstableApiUsage")
fun Item.Properties.tab(tab: DeferredSupplier<CreativeModeTab>): Item.Properties = `arch$tab`(tab)

@Suppress("UnstableApiUsage")
fun Item.Properties.tab(tab: ResourceKey<CreativeModeTab>): Item.Properties = `arch$tab`(tab)