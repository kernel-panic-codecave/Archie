package net.kernelpanicsoft.archie.config

import me.shedaniel.clothconfig2.api.ConfigBuilder
import net.kernelpanicsoft.archie.config.builder.startConfigField
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/**
 * Client-side mirror of a [ConfigContainer]. If the container holds exactly one [ConfigSpec],
 * [buildConfigContainer] opens that spec's own screen directly; with more than one, it builds a
 * screen listing an "Edit" entry per spec (via `ConfigFieldBuilder`/`ConfigSpecEntry`) that drills
 * into that spec's screen. A [ConfigSpec.Type.SERVER] entry is hidden while not in a world.
 */
class ClientConfigContainer(internal var container: ConfigContainer)
{
	fun buildConfigContainer(parent: Screen): Screen
	{
		val isWorld = Minecraft.getInstance().level != null
		val configs = container.configs
		if (configs.size == 1)
		{
			if (!isWorld && configs.first().type == ConfigSpec.Type.SERVER)
				return parent
			return configs.first().client.buildConfig(parent)
		}
		return ConfigBuilder.create().apply {
			title = container.title
			val category = getOrCreateCategory(container.title)
			configs.filter { it.type != ConfigSpec.Type.SERVER || isWorld }.forEach { config ->
				val entryBuilder = entryBuilder()
				category.addEntry(
					entryBuilder.startConfigField(config.title, config)
						.build()
				)
			}
			setFallbackCategory(category)
			parentScreen = parent
			setAfterInitConsumer { configScreen ->
				configScreen.removeWidget(configScreen.children().first { it is Button && it.message == Component.empty() })
			}
		}.build()
	}

	/** Registers this spec's config screen with the platform's mod-list UI, client-side only. */
	fun initClient() = container.mod.registerConfigurationScreen(::buildConfigContainer)
}