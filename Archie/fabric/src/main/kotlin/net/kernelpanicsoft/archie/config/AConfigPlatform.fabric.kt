package net.kernelpanicsoft.archie.config

import dev.architectury.platform.Mod
import me.shedaniel.clothconfig2.api.ConfigBuilder
import net.kernelpanicsoft.archie.config.AConfigPlatformInternal.screenHandlers
import net.minecraft.client.gui.screens.Screen

/** Fabric implementation of [AConfigPlatform]. */
actual object AConfigPlatform
{
	/** Records [builder] in [AConfigPlatformInternal.screenHandlers] for [ArchieCatalogue] to expose to Mod Menu. */
	actual fun registerScreenHandler(mod: Mod, builder: () -> (Screen) -> Screen)
	{
		screenHandlers[mod] = builder
	}
}