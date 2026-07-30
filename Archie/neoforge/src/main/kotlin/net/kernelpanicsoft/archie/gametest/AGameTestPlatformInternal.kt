package net.kernelpanicsoft.archie.gametest

import dev.architectury.platform.Mod
import net.kernelpanicsoft.archie.events.AEvents
import net.neoforged.fml.ModList
import net.neoforged.neoforge.event.RegisterGameTestsEvent

/** Backs [AGameTestPlatform] on NeoForge: holds registered test classes and drives NeoForge's own `RegisterGameTestsEvent`. */
internal object AGameTestPlatformInternal
{
	/** Test classes registered via [AGameTestPlatform.register], keyed by owning mod. */
	@JvmField
	internal val testClasses: MutableMap<Mod, MutableSet<Class<*>>> = mutableMapOf()

	/** Inverse of [testClasses]: the owning mod for each registered test class. */
	@JvmStatic
	@get:JvmName("getTestClassToMod")
	internal val testClassToMod: Map<Class<*>, Mod>
		get() = buildMap {
			testClasses.forEach { (mod, classes) ->
				classes.forEach { put(it, mod) }
			}
		}

	/**
	 * No-ops unless [AGameTestPlatform.isGameTest]. For every mod in [AEvents.MODS], subscribes to
	 * that mod's `RegisterGameTestsEvent`; when it fires, fires [AEvents.REGISTER_GAME_TEST] for the
	 * mod and registers each resulting test class with NeoForge's event.
	 */
	@JvmStatic
	@JvmName("addEventHandlers")
	fun addEventHandlers()
	{
		if (!AGameTestPlatform.isGameTest) return

		for (mod in AEvents.MODS)
		{
			ModList.get().getModContainerById(mod.modId).ifPresent {
				it.eventBus?.addListener<RegisterGameTestsEvent> { event ->
					AEvents.REGISTER_GAME_TEST.invoker()(mod)
					for (clazz in testClasses.getOrPut(mod, ::mutableSetOf))
					{
						event.register(clazz)
					}
				}
			}
		}
	}
}