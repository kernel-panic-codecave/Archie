package net.kernelpanicsoft.archie.config

import dev.architectury.platform.Mod
import me.shedaniel.clothconfig2.api.ConfigBuilder
import net.kernelpanicsoft.archie.config.AConfigPlatformInternal.screenHandlers

actual object AConfigPlatform
{
	actual fun registerScreenHandler(mod: Mod, builder: () -> ConfigBuilder)
	{
		screenHandlers[mod] = builder
	}
}