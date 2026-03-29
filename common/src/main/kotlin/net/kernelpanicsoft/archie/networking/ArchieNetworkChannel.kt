package net.kernelpanicsoft.archie.networking

import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.gui.ComposeContainerMenu
import net.kernelpanicsoft.archie.gui.blockentity.BlockEntityStatePacketRegistry
import net.kernelpanicsoft.archie.util.rem

object ArchieNetworkChannel : NetworkChannel(Archie % "main")
{
	fun init()
	{
		ComposeContainerMenu.register()
		BlockEntityStatePacketRegistry.register()
		register()
	}
}