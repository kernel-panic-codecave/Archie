package net.kernelpanicsoft.archie.registries

import dev.architectury.platform.Mod

/** Fabric has no staged registry-event model to race, so [block] just runs immediately. */
actual fun scheduleEarlyClientRegistration(mod: Mod, block: () -> Unit) = block()
