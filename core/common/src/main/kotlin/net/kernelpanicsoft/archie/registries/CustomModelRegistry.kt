package net.kernelpanicsoft.archie.registries

import dev.architectury.platform.Mod
import net.minecraft.resources.ResourceLocation

expect object CustomModelRegistry
{
	fun register(mod: Mod, id: ResourceLocation)
}