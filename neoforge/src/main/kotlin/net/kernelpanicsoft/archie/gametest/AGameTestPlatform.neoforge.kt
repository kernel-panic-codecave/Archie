package net.kernelpanicsoft.archie.gametest

import net.kernelpanicsoft.archie.events.AEvents
import dev.architectury.platform.Mod
import net.neoforged.fml.ModList
import net.neoforged.neoforge.event.RegisterGameTestsEvent
import net.neoforged.neoforge.gametest.GameTestHooks

@Suppress("unused")
actual object AGameTestPlatform
{
	actual val isGameTest: Boolean
		get() = GameTestHooks.isGametestEnabled()

	actual fun register(clazz: Class<*>, mod: Mod)
	{
		AGameTestPlatformInternal.testClasses.getOrPut(mod, ::mutableListOf).add(clazz)
	}
}
