package com.withertech.archie.gametest

import com.withertech.archie.events.AEvents
import com.withertech.archie.events.AEventObject
import dev.architectury.event.Event
import dev.architectury.platform.Mod

abstract class AGameTestEventObject(mod: Mod) :
	AEventObject<AEvents.ArchieGameTestBuilder, AEvents.RegisterGameTestHandler, AEvents.RegisterGameTestHandler.Companion>(
		mod
	)
{
	override val event: Event<AEvents.RegisterGameTestHandler> = AEvents.REGISTER_GAME_TEST
	override val handlerConstructor: AEvents.RegisterGameTestHandler.Companion = AEvents.RegisterGameTestHandler.Companion
}