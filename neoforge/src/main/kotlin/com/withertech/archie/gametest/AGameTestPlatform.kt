package com.withertech.archie.gametest

import com.withertech.archie.events.AEvents
import dev.architectury.platform.Mod
import net.neoforged.fml.ModList
import net.neoforged.neoforge.event.RegisterGameTestsEvent
import net.neoforged.neoforge.gametest.GameTestHooks

@Suppress("unused")
actual object AGameTestPlatform
{
	actual val isGameTest: Boolean
		get() = GameTestHooks.isGametestEnabled()

	actual fun register(clazz: Class<*>, mod: Mod)
	{
		testClasses.getOrPut(mod, ::mutableListOf).add(clazz)
	}

	@JvmField
	internal val testClasses: MutableMap<Mod, MutableList<Class<*>>> = mutableMapOf()

	internal val testClassToMod: Map<Class<*>, Mod> get() {
			return buildMap {
				testClasses.forEach { (key, value) ->
					value.forEach {
						put(it, key)
					}
				}
			}
		}

	@JvmStatic
	@JvmName("addEventHandlers")
	internal fun addEventHandlers()
	{
		if (isGameTest)
		{
			for (mod in AEvents.MODS)
			{
				ModList.get().getModContainerById(mod.modId).ifPresent {
					it.eventBus?.addListener<RegisterGameTestsEvent> { event ->
						AEvents.REGISTER_GAME_TEST.invoker()(mod)
						for (clazz in testClasses.getOrPut(mod, ::mutableListOf))
						{
							event.register(clazz)
						}
					}
				}
			}
		}
	}
}
