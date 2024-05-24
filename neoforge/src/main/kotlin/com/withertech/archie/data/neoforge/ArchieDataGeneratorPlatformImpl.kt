package com.withertech.archie.data.neoforge

import com.withertech.archie.data.ArchieDataGenerator
import com.withertech.archie.data.ArchieDataGeneratorPlatform
import com.withertech.archie.events.ArchieEvents
import net.neoforged.fml.ModList
import net.neoforged.neoforge.data.event.GatherDataEvent

object ArchieDataGeneratorPlatformImpl
{
	@JvmStatic
	val isDataGen: Boolean
		get() = System.getProperty("archie.datagen").toBoolean()

	@JvmStatic
	@JvmName("addEventHandlers")
	internal fun addEventHandlers()
	{
		if (ArchieDataGeneratorPlatform.isDataGen)
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