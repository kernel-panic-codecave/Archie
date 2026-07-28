package net.kernelpanicsoft.archie.gametest

import dev.architectury.platform.Mod
import dev.architectury.platform.Platform
import net.neoforged.neoforge.gametest.GameTestHooks

@Suppress("unused")
actual object AGameTestPlatform
{
	private const val SIDE_OVERRIDE_PROP = "archie.gametest.side"

	actual val isGameTest: Boolean
		get() = GameTestHooks.isGametestEnabled()

	actual val side: AGameTestSide?
		get() {
			val override = System.getProperty(SIDE_OVERRIDE_PROP)?.trim()?.lowercase()
			return when (override) {
				"client" -> AGameTestSide.CLIENT
				"server" -> AGameTestSide.SERVER
				else -> null
			}
		}

	val testClasses: MutableMap<Mod, MutableSet<Class<*>>>
		get() = AGameTestPlatformInternal.testClasses

	actual fun register(clazz: Class<*>, mod: Mod)
	{
		testClasses.getOrPut(mod, ::mutableSetOf).add(clazz)
	}
}
