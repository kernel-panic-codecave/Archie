package net.kernelpanicsoft.archie.gametest.platform

import dev.architectury.platform.Mod

/**
 * Backs [AGameTestPlatform] on NeoForge: holds registered test classes.
 *
 * Trimmed to the [testClasses] map [AGameTestPlatform.register] needs - the actual
 * `registerGameTests()` driving logic is gametest-run-only and lives in `archie-gametest` instead.
 */
internal object AGameTestPlatformInternal
{
	/** Test classes registered via [AGameTestPlatform.register], keyed by owning mod. */
	@JvmField
	internal val testClasses: MutableMap<Mod, MutableSet<Class<*>>> = mutableMapOf()
}
