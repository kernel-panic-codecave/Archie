package net.kernelpanicsoft.archie.gametest

import dev.architectury.platform.Mod

/**
 * Cross-loader GameTest integration point.
 *
 * Implementations detect whether GameTest mode is active and collect test classes per mod.
 */
expect object AGameTestPlatform
{
	val isGameTest: Boolean

	/** Register a test class for [mod] when GameTest bootstrapping occurs. */
	fun register(clazz: Class<*>, mod: Mod)
}
