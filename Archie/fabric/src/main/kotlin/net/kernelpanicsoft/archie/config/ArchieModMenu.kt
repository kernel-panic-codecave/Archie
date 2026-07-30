package net.kernelpanicsoft.archie.config

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi

/**
 * Mod Menu entrypoint, registered as the `modmenu` entrypoint in `fabric.mod.json`.
 *
 * Mod Menu discovers this once at its own init (a harmless classloading step), but calls
 * [getProvidedConfigScreenFactories] lazily each time it actually needs a mod's config screen -
 * not snapshotted once at Mod Menu's own init - so this always reflects whatever's been
 * registered via [AConfigPlatform.registerScreenHandler] by then, regardless of which mod's
 * Fabric entrypoint happens to run first.
 */
@Suppress("unused")
object ArchieModMenu : ModMenuApi
{
	override fun getProvidedConfigScreenFactories(): Map<String, ConfigScreenFactory<*>>
	{
		return AConfigPlatformInternal.screenHandlers
			.mapKeys { (key, _) -> key.modId }
			.mapValues { (_, value) ->
				ConfigScreenFactory { parent ->
					value()(parent)
				}
			}
	}
}
