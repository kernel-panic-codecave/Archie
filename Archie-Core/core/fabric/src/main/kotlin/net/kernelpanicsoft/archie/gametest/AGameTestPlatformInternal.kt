package net.kernelpanicsoft.archie.gametest

import dev.architectury.platform.Mod

/**
 * Backs [AGameTestPlatform] on Fabric: holds registered test classes.
 *
 * Trimmed to the [testClasses] map [AGameTestPlatform.register] needs - the actual
 * `registerGameTests()` driving logic (firing [net.kernelpanicsoft.archie.events.AEvents
 * .REGISTER_GAME_TEST], wiring `GameTestRegistry`/`FabricGameTestModInitializerMixin`) is
 * gametest-run-only and lives in `archie-gametest` instead.
 */
internal object AGameTestPlatformInternal
{
	/** Test classes registered via [AGameTestPlatform.register], keyed by owning mod. */
	@JvmField
	internal val testClasses: MutableMap<Mod, MutableSet<Class<*>>> = mutableMapOf()
}
