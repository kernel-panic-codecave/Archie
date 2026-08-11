package net.kernelpanicsoft.archie.data

import net.kernelpanicsoft.archie.events.ADatagenEvents
import net.neoforged.fml.ModList
import net.neoforged.neoforge.data.event.GatherDataEvent

/** Backs [ADataGeneratorPlatform] on NeoForge: wires each registered mod's [GatherDataEvent] listener into [ADatagenEvents.GATHER_DATA]. */
internal object ADataGeneratorPlatformInternal
{
	/**
	 * Called from `DatagenModLoaderMixin` mid-way through NeoForge's datagen bootstrap. No-ops
	 * outside a datagen run; otherwise, for every mod in [ADatagenEvents.MODS], subscribes to
	 * that mod's [GatherDataEvent] and fires [ADatagenEvents.GATHER_DATA] with an
	 * [ADataGeneratorNeoForge] wrapping it.
	 */
	@JvmStatic
	@JvmName("addEventHandlers")
	fun addEventHandlers()
	{
		if (!ADataGeneratorPlatform.isDataGen) return

		for (mod in ADatagenEvents.MODS)
		{
			ModList.get().getModContainerById(mod.modId).ifPresent {
				it.eventBus?.addListener<GatherDataEvent> { event ->
					ADatagenEvents.GATHER_DATA.invoker()(ADataGeneratorNeoForge(event, mod))
				}
			}
		}
	}
}
