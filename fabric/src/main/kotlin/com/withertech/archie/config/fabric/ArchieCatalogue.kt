package com.withertech.archie.config.fabric

import net.fabricmc.loader.api.ModContainer
import net.minecraft.client.gui.screens.Screen
import java.util.function.BiFunction

object ArchieCatalogue
{
	@JvmStatic
	fun createConfigProvider(): Map<String, BiFunction<Screen, ModContainer, Screen>>
	{
		return ArchieConfigPlatformImpl.screenHandlers.mapKeys {
			it.key.modId
		}.mapValues {
			BiFunction<Screen, ModContainer, Screen> { screen, _ -> it.value().setParentScreen(screen).build() }
		}
	}
}