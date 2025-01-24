package net.kernelpanicsoft.archie.gametest

import dev.architectury.platform.Mod
import net.kernelpanicsoft.archie.events.AEvents
import net.neoforged.fml.ModList
import net.neoforged.neoforge.event.RegisterGameTestsEvent

internal object AGameTestPlatformInternal
{
	@JvmField
	internal val testClasses: MutableMap<Mod, MutableList<Class<*>>> = mutableMapOf()

	@JvmStatic
	@get:JvmName("getTestClassToMod")
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
	fun addEventHandlers()
	{
		if (AGameTestPlatform.isGameTest)
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