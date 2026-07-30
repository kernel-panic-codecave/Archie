package net.kernelpanicsoft.archie.config

import dev.architectury.platform.Mod
import me.shedaniel.clothconfig2.api.ConfigBuilder
import net.minecraft.client.gui.screens.Screen
import net.neoforged.fml.ModList
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import java.util.function.Supplier

/** NeoForge implementation of [AConfigPlatform]. */
actual object AConfigPlatform
{
	/** Registers [builder] as [mod]'s `IConfigScreenFactory` extension point, so NeoForge's mod list "Config" button opens it. */
	actual fun registerScreenHandler(mod: Mod, builder: () -> (Screen) -> Screen)
	{
		ModList.get().getModContainerById(mod.modId).ifPresent { container ->
			container.registerExtensionPoint(IConfigScreenFactory::class.java) {
				IConfigScreenFactory { _, screen ->
					builder()(screen)
				}
			}
		}
	}
}