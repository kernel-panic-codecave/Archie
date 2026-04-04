package net.kernelpanicsoft.archie.config

import dev.architectury.platform.Mod
import me.shedaniel.clothconfig2.api.ConfigBuilder
import net.minecraft.client.gui.screens.Screen

expect object AConfigPlatform
{
	fun registerScreenHandler(mod: Mod, builder: () -> (Screen) -> Screen)
}