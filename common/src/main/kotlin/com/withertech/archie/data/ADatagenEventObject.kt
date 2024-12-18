package com.withertech.archie.data

import com.withertech.archie.events.AEvents
import com.withertech.archie.events.AEvents.GatherDataHandler
import com.withertech.archie.events.AEventObject
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