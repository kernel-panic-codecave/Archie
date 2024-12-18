package com.withertech.archie.data

import com.withertech.archie.events.AEvents
import net.neoforged.fml.ModList
import net.neoforged.neoforge.data.event.GatherDataEvent

actual object ADataGeneratorPlatform
{
	actual val isDataGen: Boolean
		get() = System.getProperty("archie.datagen").toBoolean()

	@JvmStatic
	@JvmName("addEventHandlers")
	internal fun addEventHandlers()
	{
		if (isDataGen)
		{
			for (mod in AEvents.MODS)
			{

				ModList.get().getModContainerById(mod.modId).ifPresent {
					it.eventBus?.addListener<GatherDataEvent> { event ->
						AEvents.GATHER_DATA.invoker()(ADataGeneratorNeoForge(event, mod))
					}
				}
			}
		}
	}
}