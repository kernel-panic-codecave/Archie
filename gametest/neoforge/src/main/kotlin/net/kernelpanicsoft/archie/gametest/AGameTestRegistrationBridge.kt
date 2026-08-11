package net.kernelpanicsoft.archie.gametest

import dev.architectury.platform.Mod
import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.events.gametest.AGametestEvents
import net.kernelpanicsoft.archie.gametest.platform.AGameTestModFilter
import net.kernelpanicsoft.archie.gametest.platform.AGameTestPlatform
import net.neoforged.fml.ModList
import net.neoforged.neoforge.event.RegisterGameTestsEvent

/**
 * Backs [AGameTestPlatform] on NeoForge: drives NeoForge's own `RegisterGameTestsEvent`. Named
 * distinctly from `archie-core`'s own (internal, `testClasses`-only) `AGameTestPlatformInternal`
 * to avoid a same-package class name collision on the runtime classpath - this reaches
 * `archie-core`'s test class map via [AGameTestPlatform.testClasses] instead of touching that
 * internal object directly.
 */
object AGameTestRegistrationBridge
{
	/**
	 * Inverse of [AGameTestPlatform.testClasses]: the owning mod for each registered test class.
	 * Used by `GameTestHooksMixin` to resolve a `@GameTest`-annotated method's template namespace
	 * when its `template()` string doesn't specify one explicitly.
	 */
	@JvmStatic
	@get:JvmName("getTestClassToMod")
	val testClassToMod: Map<Class<*>, Mod>
		get() = buildMap {
			AGameTestPlatform.testClasses.forEach { (mod, classes) ->
				classes.forEach { put(it, mod) }
			}
		}

	/**
	 * No-ops unless [AGameTestPlatform.isGameTest]. For every mod selected by
	 * [AGameTestModFilter] from [AGametestEvents.MODS] (or just [Archie.MOD] if that's empty),
	 * subscribes to that mod's `RegisterGameTestsEvent`; when it fires, fires
	 * [AGametestEvents.REGISTER_GAME_TEST] for the mod and registers each resulting test class
	 * with NeoForge's event.
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

		for (mod in AGameTestModFilter.selectMods(AGametestEvents.MODS.ifEmpty { listOf(Archie.MOD) }))
		{
			ModList.get().getModContainerById(mod.modId).ifPresent {
				it.eventBus?.addListener<RegisterGameTestsEvent> { event ->
					AGametestEvents.REGISTER_GAME_TEST.invoker()(mod)
					val classes = AGameTestPlatform.testClasses.getOrPut(mod, ::mutableSetOf)
					for (clazz in if (classes.isEmpty()) setOf(NoOpGameTest::class.java) else classes)
					{
						event.register(clazz)
					}
				}
			}
		}
	}
}
