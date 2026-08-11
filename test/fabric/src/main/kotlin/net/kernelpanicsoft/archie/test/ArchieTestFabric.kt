package net.kernelpanicsoft.archie.test

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer

object ArchieTestFabric : ModInitializer, ClientModInitializer
{
	override fun onInitialize()
	{
		ArchieTest.init()
		ArchieTest.initCommon()
	}

	override fun onInitializeClient()
	{
		ArchieTest.initClient()
	}
}