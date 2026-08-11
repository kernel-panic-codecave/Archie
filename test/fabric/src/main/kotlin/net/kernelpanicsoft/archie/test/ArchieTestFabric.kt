package net.kernelpanicsoft.archie.test

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import net.kernelpanicsoft.archie.gametest.platform.AGameTestPlatform
import net.kernelpanicsoft.archie.test.gametest.CapabilityLookupTestFixtures

object ArchieTestFabric : ModInitializer, ClientModInitializer
{
	override fun onInitialize()
	{
		ArchieTest.init()
		ArchieTest.initCommon()
		// See the matching comment in ArchieTest.init() - Fabric's registries resolve immediately
		// on registration (no staged/frozen model to race), so this is safe right here.
		if (AGameTestPlatform.isGameTest) CapabilityLookupTestFixtures.init()
	}

	override fun onInitializeClient()
	{
		ArchieTest.initClient()
	}
}