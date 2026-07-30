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
	 * No-ops unless [isGameTest]. Fires [AEvents.REGISTER_GAME_TEST] for every registered mod (or
	 * just [Archie.MOD] if none registered), then registers each resulting test class with
	 * [GameTestRegistry] and [FabricGameTestModInitializerMixin]'s id/logger bookkeeping - throwing
	 * if the same class is registered under more than one mod.
	 */
	@JvmStatic
	@JvmName("registerGameTests")
	internal fun registerGameTests()
	{
		if (!isGameTest) return
		Archie.LOGGER.info("Registering GameTests")
		GlobalTestReporter.replaceWith(VerboseTestReporter)
		val mods = AEvents.MODS.ifEmpty { listOf(Archie.MOD) }
		for (mod in mods)
		{
			AEvents.REGISTER_GAME_TEST.invoker()(mod)
			for (clazz in testClasses.getOrPut(mod, ::mutableSetOf))
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