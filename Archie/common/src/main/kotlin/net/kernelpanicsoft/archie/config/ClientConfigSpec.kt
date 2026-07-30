package net.kernelpanicsoft.archie.config

import net.kernelpanicsoft.archie.config.serializer.Json5ConfigSerializer
import net.kernelpanicsoft.archie.config.serializer.TomlConfigSerializer
import net.kernelpanicsoft.archie.APlatform
import dev.architectury.platform.Mod
import dev.architectury.platform.Platform
import dev.architectury.utils.Env
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import me.shedaniel.clothconfig2.api.ConfigBuilder
import net.minecraft.network.chat.Component

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
		if (Platform.getEnvironment() == Env.CLIENT)
		{
			AConfigPlatform.registerScreenHandler(spec.mod) {
				{
					builder.parentScreen = it
					builder.build()
				}
			}
		}
	}
}