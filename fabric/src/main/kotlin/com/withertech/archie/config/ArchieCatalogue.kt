package com.withertech.archie.config

import net.fabricmc.loader.api.ModContainer
import net.minecraft.client.gui.screens.Screen
import java.util.function.BiFunction

@Suppress("unused")
object ArchieCatalogue
{
	@JvmStatic
	fun createConfigProvider(): Map<String, BiFunction<Screen, ModContainer, Screen>>
	{
		return ArchieConfigPlatform.screenHandlers
			.mapKeys { (key, _) -> key.modId }
			.mapValues { (_, value) ->
				BiFunction { parent, _ ->
					value.invoke().setParentScreen(parent).build()
				}
			}
	}
}