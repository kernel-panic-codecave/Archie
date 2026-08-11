package net.kernelpanicsoft.archie.registries

import dev.architectury.registry.CreativeTabRegistry
import net.minecraft.world.item.CreativeModeTab

/** Thin wrapper over Architectury's [CreativeTabRegistry] for one-off creative tab creation. */
object ACreativeTabRegistry
{
	/** Builds a [CreativeModeTab] via [block] without registering it. See [CreativeTabRegistryHelper] to register one. */
	fun create(block: CreativeModeTab.Builder.() -> Unit): CreativeModeTab = CreativeTabRegistry.create(block)
}