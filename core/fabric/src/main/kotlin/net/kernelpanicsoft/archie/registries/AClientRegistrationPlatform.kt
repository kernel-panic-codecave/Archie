package net.kernelpanicsoft.archie.registries

import dev.architectury.platform.Mod
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey

/** Fabric has no staged registry-event model to race, so [block] just runs immediately. */
actual fun scheduleEarlyClientRegistration(mod: Mod, registryKey: ResourceKey<out Registry<*>>, block: () -> Unit) = block()
