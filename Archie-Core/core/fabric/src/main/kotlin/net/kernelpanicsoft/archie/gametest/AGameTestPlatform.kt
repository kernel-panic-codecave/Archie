package net.kernelpanicsoft.archie.gametest

import dev.architectury.platform.Mod
import dev.architectury.platform.Platform

/** Fabric implementation of [AGameTestPlatform]. */
@Suppress("unused")
actual object AGameTestPlatform
{
	private const val SIDE_OVERRIDE_PROP = "archie.gametest.side"
	private const val GAMETEST_PROP = "archie.gametest"

	/**
	 * True when running under one of Archie's own GameTest run configs.
	 *
	 * Deliberately reads [GAMETEST_PROP] instead of `FabricGameTestHelper.ENABLED` - Loom's
	 * generated dev-launch config shares a single property bucket per environment, so a flag set
	 * on the `gametestClient` run can leak into the plain `client` run's bucket too (observed on
	 * the NeoForge side; kept consistent here). [GAMETEST_PROP] is a property Archie's own build
	 * sets exclusively on its `gametest`/`gametestClient` runs, so it isn't affected by that leak.
	 */
	actual val isGameTest: Boolean
		get() = System.getProperty(GAMETEST_PROP)?.toBoolean() == true

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
