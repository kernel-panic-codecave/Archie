package net.kernelpanicsoft.archie.events

import dev.architectury.event.Event
import dev.architectury.platform.Mod

abstract class AEventObject<T, H : AEvents.Handler<T>, C : AEvents.HandlerConstructor<T, H>>(val mod: Mod)
{
	abstract val event: Event<H>

	@Volatile
	private var initialized: Boolean = false
	
	abstract val handlerConstructor: C
	
	abstract fun T.handler()
	
	fun init()
	{
		if (initialized) return
		synchronized(this)
		{
			if (initialized) return
			event.register(handlerConstructor.create(mod) { handler() })
			initialized = true
		}
	}
}