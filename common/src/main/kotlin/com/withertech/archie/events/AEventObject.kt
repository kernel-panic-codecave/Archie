package com.withertech.archie.events

import dev.architectury.event.Event
import dev.architectury.platform.Mod

abstract class AEventObject<T, H : AEvents.Handler<T>, C : AEvents.HandlerConstructor<T, H>>(val mod: Mod)
{
	abstract val event: Event<H>
	
	abstract val handlerConstructor: C
	
	abstract fun T.handler()
	
	fun init()
	{
		event.register(handlerConstructor.create(mod) {handler()})
	}
}