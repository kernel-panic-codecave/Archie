package net.kernelpanicsoft.archie.registries

import dev.architectury.registry.CreativeTabRegistry
import net.minecraft.world.item.CreativeModeTab

object ACreativeTabRegistry
{
	fun create(block: CreativeModeTab.Builder.() -> Unit): CreativeModeTab = CreativeTabRegistry.create(block)
}