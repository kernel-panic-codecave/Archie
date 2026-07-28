package net.kernelpanicsoft.archie.gametest

import dev.architectury.platform.Mod
import dev.architectury.utils.Env

/** Logical side used while collecting/running GameTests. */
enum class AGameTestSide {
	SERVER,
	CLIENT,
}

internal fun AGameTestSide.toEnv(): Env = when (this) {
	AGameTestSide.SERVER -> Env.SERVER
	AGameTestSide.CLIENT -> Env.CLIENT
}

/**
 * Cross-loader GameTest integration point.
 *
 * Implementations detect whether GameTest mode is active and collect test classes per mod.
 */
expect object AGameTestPlatform
{
	val isGameTest: Boolean

	/** Active logical side for this GameTest run (supports launcher/property overrides). */
	val side: AGameTestSide?

	/** Register a test class for [mod] when GameTest bootstrapping occurs. */
	fun register(clazz: Class<*>, mod: Mod)
}
