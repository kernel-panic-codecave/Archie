package net.kernelpanicsoft.archie.config

import dev.architectury.platform.Mod
import me.shedaniel.clothconfig2.api.ConfigBuilder
import net.minecraft.client.gui.screens.Screen

/**
 * Platform hook for exposing a mod's config screen to its loader's mod-list UI (Mod Menu on
 * Fabric, the built-in config button on NeoForge).
 */
expect object AConfigPlatform
{
	/**
	 * Registers [builder] as the factory for [mod]'s config screen, given the screen to return to.
	 * Called by [ClientConfigSpec.initClient].
	 */
	fun registerScreenHandler(mod: Mod, builder: () -> (Screen) -> Screen)
}