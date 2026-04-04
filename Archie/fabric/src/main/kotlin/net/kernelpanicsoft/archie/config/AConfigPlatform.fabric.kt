package net.kernelpanicsoft.archie.config

import dev.architectury.platform.Mod
import me.shedaniel.clothconfig2.api.ConfigBuilder
import net.kernelpanicsoft.archie.config.AConfigPlatformInternal.screenHandlers
import net.minecraft.client.gui.screens.Screen

actual object AConfigPlatform
{
	actual fun registerScreenHandler(mod: Mod, builder: () -> (Screen) -> Screen)
	{
		screenHandlers[mod] = builder
	}
}