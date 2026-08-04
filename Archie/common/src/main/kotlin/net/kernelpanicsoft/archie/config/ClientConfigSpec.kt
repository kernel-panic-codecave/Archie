package net.kernelpanicsoft.archie.config

import me.shedaniel.clothconfig2.api.ConfigBuilder
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen

/** Client-side mirror of a [ConfigSpec], built lazily as [ConfigSpec.client]; builds the Cloth Config UI screen. */
@Suppress("unused")
class ClientConfigSpec(internal var spec: ConfigSpec)
{
	/**
	 * Builds a fresh Cloth Config [ConfigBuilder] for [spec]: one category per enabled entry of
	 * [ConfigSpec.categoriesMap]. Saving writes locally via [ConfigSpec.save] for
	 * [ConfigSpec.Type.COMMON]/[ConfigSpec.Type.CLIENT]/[ConfigSpec.Type.STARTUP] specs, or sends
	 * the edited config to the server over [ConfigSpec.channel] for [ConfigSpec.Type.SERVER] specs.
	 */
	fun buildConfig(parent: Screen): Screen
	{
		return ConfigBuilder.create().apply {
			title = spec.title
			savingRunnable = Runnable {
				when (spec.type)
				{
					ConfigSpec.Type.COMMON,
					ConfigSpec.Type.CLIENT,
					ConfigSpec.Type.STARTUP -> spec.save()
					ConfigSpec.Type.SERVER -> spec.channel.toServer(spec)
				}
			}
			spec.categoriesMap.values.forEach { value ->
				if (value.isEnabled)
				{
					val entryBuilder = entryBuilder()
					val category = getOrCreateCategory(value.title)

					value.client.buildRoot(category, entryBuilder)
				}
			}
			parentScreen = parent
		}.build()
	}

}