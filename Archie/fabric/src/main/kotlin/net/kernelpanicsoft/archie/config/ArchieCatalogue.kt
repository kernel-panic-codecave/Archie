package net.kernelpanicsoft.archie.config

import net.fabricmc.loader.api.ModContainer
import net.minecraft.client.gui.screens.Screen
import java.util.function.BiFunction

/**
 * Mod Menu config-screen entrypoint, referenced as `configFactory` in `fabric.mod.json`.
 *
 * Mod Menu discovers this class by name and calls [createConfigProvider] to get one config
 * screen factory per Archie-based mod that registered via [AConfigPlatform.registerScreenHandler].
 */
@Suppress("unused")
object ArchieCatalogue
{
	/** Maps each registered mod's id to a factory building its config [Screen] from the parent screen. */
	@JvmStatic
	fun createConfigProvider(): Map<String, BiFunction<Screen, ModContainer, Screen>>
	{
		return AConfigPlatformInternal.screenHandlers
			.mapKeys { (key, _) -> key.modId }
			.mapValues { (_, value) ->
				BiFunction { parent, _ ->
					value()(parent)
				}
			}
	}
}