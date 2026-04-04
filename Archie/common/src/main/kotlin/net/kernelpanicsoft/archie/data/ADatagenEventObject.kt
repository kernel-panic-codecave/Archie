package net.kernelpanicsoft.archie.data

import net.kernelpanicsoft.archie.events.AEvents
import net.kernelpanicsoft.archie.events.AEvents.GatherDataHandler
import net.kernelpanicsoft.archie.events.AEventObject
import dev.architectury.event.Event
import dev.architectury.platform.Mod

abstract class ADatagenEventObject(mod: Mod) :
	AEventObject<ADataGenerator, GatherDataHandler, GatherDataHandler.Companion>(
		mod
	)
{
	override val event: Event<GatherDataHandler> = AEvents.GATHER_DATA
	override val handlerConstructor: GatherDataHandler.Companion = GatherDataHandler.Companion
}