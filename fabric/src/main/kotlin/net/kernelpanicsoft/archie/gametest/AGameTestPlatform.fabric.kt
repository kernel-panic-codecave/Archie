package net.kernelpanicsoft.archie.gametest

import dev.architectury.platform.Mod
import net.fabricmc.fabric.impl.gametest.FabricGameTestHelper

@Suppress("unused")
actual object AGameTestPlatform
{
	@Suppress("UnstableApiUsage")
	actual val isGameTest: Boolean
		get() = FabricGameTestHelper.ENABLED

	actual fun register(clazz: Class<*>, mod: Mod)
	{
		testClasses.getOrPut(mod, ::mutableListOf).add(clazz)
	}

	@JvmField
	internal val testClasses: MutableMap<Mod, MutableList<Class<*>>> = mutableMapOf()


}
