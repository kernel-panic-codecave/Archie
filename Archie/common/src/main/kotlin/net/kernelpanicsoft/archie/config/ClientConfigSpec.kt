package net.kernelpanicsoft.archie.config

import me.shedaniel.clothconfig2.api.ConfigBuilder
import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.util.onClient

/** Client-side mirror of a [ConfigSpec], built lazily as [ConfigSpec.client]; builds the Cloth Config UI screen. */
@Suppress("unused")
class ClientConfigSpec(internal var spec: ConfigSpec)
{
	/** Builds a fresh Cloth Config [ConfigBuilder] for [spec]: one category per enabled entry of [ConfigSpec.categoriesMap], saving via [ConfigSpec.save]. */
	val builder: ConfigBuilder
		get()
		{
			val configBuilder = ConfigBuilder.create()
			configBuilder.title = spec.title
			configBuilder.savingRunnable = Runnable {
				spec.save()
			}
			spec.categoriesMap.values.forEach { value ->
				if (value.isEnabled)
				{
					val entryBuilder = configBuilder.entryBuilder()
					val category = configBuilder.getOrCreateCategory(value.title)

					value.client.buildRoot(category, entryBuilder)
				}
			}
			return configBuilder
		}

	/** Registers this spec's config screen with the platform's mod-list UI, client-side only. */
	fun initClient()
	{
		onClient {
			spec.mod.registerConfigurationScreen {
				Archie.LOGGER.info("Registering config screen")
				builder.parentScreen = it
				builder.build()
			}
		}
	}
}