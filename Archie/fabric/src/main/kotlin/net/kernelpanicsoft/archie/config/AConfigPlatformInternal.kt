package net.kernelpanicsoft.archie.config

import dev.architectury.platform.Mod
import me.shedaniel.clothconfig2.api.ConfigBuilder
import net.minecraft.client.gui.screens.Screen

/** Holds per-mod config-screen builders registered via [AConfigPlatform.registerScreenHandler], for [ArchieCatalogue] to read. */
internal object AConfigPlatformInternal
{
	@JvmField
	internal val screenHandlers: MutableMap<Mod, () -> (Screen) -> Screen> = mutableMapOf()
}