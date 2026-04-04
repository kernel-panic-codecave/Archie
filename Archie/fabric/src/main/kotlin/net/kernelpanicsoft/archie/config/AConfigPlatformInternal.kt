package net.kernelpanicsoft.archie.config

import dev.architectury.platform.Mod
import me.shedaniel.clothconfig2.api.ConfigBuilder
import net.minecraft.client.gui.screens.Screen

internal object AConfigPlatformInternal
{
	@JvmField
	internal val screenHandlers: MutableMap<Mod, () -> (Screen) -> Screen> = mutableMapOf()
}