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
	 * [ConfigSpec.Type.COMMON]/[ConfigSpec.Type.CLIENT]/[ConfigSpec.Type.STARTUP] specs.
	 *
	 * A [ConfigSpec.Type.SERVER] spec is the server's to own, so an edit is normally sent to it -
	 * over a [ConfigSpecCollection]'s shared channel if [spec] is one of its entries
	 * ([ConfigSpec.collectionSync]), or over [ConfigSpec.channel] directly otherwise - and applied
	 * only if the server accepts it.
	 *
	 * **Unless this client is the server.** With an integrated server there is one of these objects,
	 * not two: the UI has already written the edit into the very instance the server ticks, and
	 * sending it would decode it back onto itself - having first put it through the handler's
	 * permission check, which a single-player host fails outright unless it opened the world to LAN
	 * with cheats on. So the host writes the file directly and pushes the result to the world, on the
	 * server thread, since other players are who the round trip existed to inform. The host is on
	 * the receiving end of that push as well - see the clientbound handler in [ConfigSpec], which is
	 * where being told your own news is made harmless.
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
					ConfigSpec.Type.SERVER -> when (val integrated = Minecraft.getInstance().singleplayerServer)
					{
						null -> spec.collectionSync?.invoke() ?: spec.channel.toServer(spec)
						else ->
						{
							spec.save()
							// A collection entry has no channel of its own to broadcast on (see
							// ConfigSpec.partOfCollection); the host's own file is still written.
							if (!spec.partOfCollection) integrated.execute { spec.channel.toAllPlayers(spec) }
						}
					}
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