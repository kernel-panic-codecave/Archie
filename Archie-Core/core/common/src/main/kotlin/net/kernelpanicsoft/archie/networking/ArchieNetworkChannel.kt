package net.kernelpanicsoft.archie.networking

import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.gui.ComposeContainerMenuBase
import net.kernelpanicsoft.archie.gui.blockentity.BlockEntityStatePacketRegistry
import net.kernelpanicsoft.archie.gui.item.ItemStatePacketRegistry
import net.kernelpanicsoft.archie.util.rem

/**
 * Archie's own [NetworkChannel], used for its internal packets (Compose container menu slot
 * syncing, block entity/item state syncing). Not intended for use by downstream mods; create your
 * own [NetworkChannel] instance instead.
 */
object ArchieNetworkChannel : NetworkChannel(Archie % "main")
{
	/**
	 * Registers Archie's built-in packet handlers, then [register]s the channel. Called once
	 * from [Archie.init].
	 */
	fun init()
	{
		ComposeContainerMenuBase.register()
		BlockEntityStatePacketRegistry.register()
		ItemStatePacketRegistry.register()
		register()
	}
}