package net.kernelpanicsoft.archie.gametest

import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.events.AGametestEvents
import net.kernelpanicsoft.archie.gametest.AGameTestPlatform.isGameTest
import net.kernelpanicsoft.archie.mixin.fabric.FabricGameTestModInitializerMixin
import net.minecraft.gametest.framework.GameTestRegistry
import net.minecraft.gametest.framework.GlobalTestReporter

/**
 * Backs `FabricGameTestHelperMixin`, which calls [registerGameTests] at the head of Fabric's
 * `FabricGameTestHelper.runHeadlessServer` - the flush step that drives Fabric's own GameTest
 * registry. Named distinctly from `archie-core`'s own (internal, `testClasses`-only)
 * `AGameTestPlatformInternal` to avoid a same-package class name collision on the runtime
 * classpath - this reaches `archie-core`'s test class map via [AGameTestPlatform.testClasses]
 * instead of touching that internal object directly.
 */
object AGameTestRegistrationBridge
{
	/**
	 * No-ops unless [isGameTest]. Fires [AGametestEvents.REGISTER_GAME_TEST] for every mod
	 * selected by [AGameTestModFilter] (or just [Archie.MOD] if [AGametestEvents.MODS] is empty),
	 * then registers each resulting test class with [GameTestRegistry] and
	 * [FabricGameTestModInitializerMixin]'s id/logger bookkeeping - throwing if the same class is
	 * registered under more than one mod.
	 *
	 * Falls back to [NoOpGameTest] for a mod whose registration turns up no classes at all for the
	 * current [AGameTestPlatform.side] (e.g. a client-only mod's server invocation) - vanilla's
	 * `GameTestServer` refuses to boot with zero test functions registered anywhere.
	 */
	@JvmStatic
	fun registerGameTests()
	{
		if (!isGameTest) return
		Archie.LOGGER.info("Registering GameTests")
		GlobalTestReporter.replaceWith(VerboseTestReporter)
		val mods = AGameTestModFilter.selectMods(AGametestEvents.MODS.ifEmpty { listOf(Archie.MOD) })
		for (mod in mods)
		{
			AGametestEvents.REGISTER_GAME_TEST.invoker()(mod)
			val classes = AGameTestPlatform.testClasses.getOrPut(mod, ::mutableSetOf)
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
