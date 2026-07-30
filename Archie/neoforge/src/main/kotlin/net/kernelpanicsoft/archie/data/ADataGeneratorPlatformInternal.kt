package net.kernelpanicsoft.archie.data

import net.kernelpanicsoft.archie.events.AEvents
import net.neoforged.fml.ModList
import net.neoforged.neoforge.data.event.GatherDataEvent

/** Backs [ADataGeneratorPlatform] on NeoForge: wires each registered mod's [GatherDataEvent] listener into [AEvents.GATHER_DATA]. */
internal object ADataGeneratorPlatformInternal
{
	/**
	 * Called from `DatagenModLoaderMixin` mid-way through NeoForge's datagen bootstrap. No-ops
	 * outside a datagen run; otherwise, for every mod in [AEvents.MODS], subscribes to that mod's
	 * [GatherDataEvent] and fires [AEvents.GATHER_DATA] with an [ADataGeneratorNeoForge] wrapping it.
	 */
	@JvmStatic
	@JvmName("addEventHandlers")
	fun addEventHandlers()
	{
		if (!ADataGeneratorPlatform.isDataGen) return

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