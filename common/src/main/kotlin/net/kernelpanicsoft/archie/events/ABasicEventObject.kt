package net.kernelpanicsoft.archie.events

import dev.architectury.event.Event

abstract class ABasicEventObject<T>()
{
	abstract val event: Event<T>

	abstract val handler: T

	fun init()
	{
		event.register(handler)
	}
}