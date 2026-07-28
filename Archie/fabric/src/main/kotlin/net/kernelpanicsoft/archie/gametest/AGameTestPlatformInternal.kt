package net.kernelpanicsoft.archie.gametest

import dev.architectury.platform.Mod
import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.events.AEvents
import net.kernelpanicsoft.archie.gametest.AGameTestPlatform.isGameTest
import net.kernelpanicsoft.archie.mixin.fabric.FabricGameTestModInitializerMixin
import net.minecraft.gametest.framework.GameTestRegistry
import net.minecraft.gametest.framework.GlobalTestReporter

internal object AGameTestPlatformInternal
{

	@JvmField
	internal val testClasses: MutableMap<Mod, MutableSet<Class<*>>> = mutableMapOf()

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