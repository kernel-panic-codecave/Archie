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
	 * No-ops unless [AGameTestPlatform.isGameTest]. For every mod selected by [AGameTestModFilter]
	 * from [AEvents.MODS], subscribes to that mod's `RegisterGameTestsEvent`; when it fires, fires
	 * [AEvents.REGISTER_GAME_TEST] for the mod and registers each resulting test class with
	 * NeoForge's event.
	 *
	 * Falls back to [NoOpGameTest] for a mod whose registration turns up no classes at all for the
	 * current [AGameTestPlatform.side] (e.g. a client-only mod's server invocation) - vanilla's
	 * `GameTestServer` refuses to boot with zero test functions registered anywhere.
	 */
	@JvmStatic
	@JvmName("addEventHandlers")
	fun addEventHandlers()
	{
		if (!AGameTestPlatform.isGameTest) return

		for (mod in AGameTestModFilter.selectMods(AEvents.MODS))
		{
			ModList.get().getModContainerById(mod.modId).ifPresent {
				it.eventBus?.addListener<RegisterGameTestsEvent> { event ->
					AEvents.REGISTER_GAME_TEST.invoker()(mod)
					val classes = testClasses.getOrPut(mod, ::mutableSetOf)
					for (clazz in if (classes.isEmpty()) setOf(NoOpGameTest::class.java) else classes)
					{
						event.register(clazz)
					}
				}
			}
		}
	}
}