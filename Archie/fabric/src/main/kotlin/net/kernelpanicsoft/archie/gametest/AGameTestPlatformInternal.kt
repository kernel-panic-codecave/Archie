package net.kernelpanicsoft.archie.gametest

import dev.architectury.platform.Mod
import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.events.AEvents
import net.kernelpanicsoft.archie.gametest.AGameTestPlatform.isGameTest
import net.kernelpanicsoft.archie.mixin.fabric.FabricGameTestModInitializerMixin
import net.minecraft.gametest.framework.GameTestRegistry
import net.minecraft.gametest.framework.GlobalTestReporter

/** Backs [AGameTestPlatform] on Fabric: holds registered test classes and drives Fabric's own GameTest registry. */
internal object AGameTestPlatformInternal
{

	/** Test classes registered via [AGameTestPlatform.register], keyed by owning mod. */
	@JvmField
	internal val testClasses: MutableMap<Mod, MutableSet<Class<*>>> = mutableMapOf()

	/**
	 * No-ops unless [isGameTest]. Fires [AEvents.REGISTER_GAME_TEST] for every mod selected by
	 * [AGameTestModFilter] (or just [Archie.MOD] if [AEvents.MODS] is empty), then registers each
	 * resulting test class with [GameTestRegistry] and [FabricGameTestModInitializerMixin]'s
	 * id/logger bookkeeping - throwing if the same class is registered under more than one mod.
	 *
	 * Falls back to [NoOpGameTest] for a mod whose registration turns up no classes at all for the
	 * current [AGameTestPlatform.side] (e.g. a client-only mod's server invocation) - vanilla's
	 * `GameTestServer` refuses to boot with zero test functions registered anywhere.
	 */
	@JvmStatic
	@JvmName("registerGameTests")
	internal fun registerGameTests()
	{
		if (!isGameTest) return
		Archie.LOGGER.info("Registering GameTests")
		GlobalTestReporter.replaceWith(VerboseTestReporter)
		val mods = AGameTestModFilter.selectMods(AEvents.MODS.ifEmpty { listOf(Archie.MOD) })
		for (mod in mods)
		{
			AEvents.REGISTER_GAME_TEST.invoker()(mod)
			val classes = testClasses.getOrPut(mod, ::mutableSetOf)
			val toRegister = if (classes.isEmpty()) setOf(NoOpGameTest::class.java) else classes
			for (clazz in toRegister)
			{
				if (FabricGameTestModInitializerMixin.getGameTestIds().containsKey(clazz))
				{
					throw UnsupportedOperationException(
						"Test class (${clazz.canonicalName}) has already been registered with mod (${mod.modId})"
					)
				}

				FabricGameTestModInitializerMixin.getGameTestIds()[clazz] = mod.modId
				GameTestRegistry.register(clazz)

				FabricGameTestModInitializerMixin.getLogger().debug(
					"Registered test class {} for mod {}",
					clazz.canonicalName,
					mod.modId
				)
			}
		}
	}
}