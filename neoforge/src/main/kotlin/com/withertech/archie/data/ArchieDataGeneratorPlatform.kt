package com.withertech.archie.data

import com.withertech.archie.events.ArchieEvents
import net.neoforged.fml.ModList
import net.neoforged.neoforge.data.event.GatherDataEvent

actual object ArchieDataGeneratorPlatform
{
	actual val isDataGen: Boolean
		get() = System.getProperty("archie.datagen").toBoolean()

	@JvmStatic
	@JvmName("addEventHandlers")
	internal fun addEventHandlers()
	{
		if (isDataGen)
		{
			for (mod in ArchieEvents.MODS)
			{

				ModList.get().getModContainerById(mod.modId).ifPresent {
					it.eventBus?.addListener<GatherDataEvent> { event ->
						ArchieEvents.GATHER_DATA.invoker()(ArchieDataGeneratorNeoForge(event, mod))
					}
				}
			}
		}
	}
}