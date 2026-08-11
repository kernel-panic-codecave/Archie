package net.kernelpanicsoft.archie.events

import dev.architectury.event.Event

/**
 * A simpler alternative to [AEventObject] for wrapping an Architectury [event] that isn't
 * scoped to a particular [dev.architectury.platform.Mod] and doesn't need a
 * [HandlerConstructor].
 *
 * @param T The Architectury handler/listener type expected by [event].
 */
abstract class ABasicEventObject<T>()
{
	/** The underlying Architectury event this wrapper registers [handler] with. */
	abstract val event: Event<T>

	/** The listener registered with [event] by [init]. */
	abstract val handler: T

	/** Registers [handler] with [event]. Not idempotent; call once during initialization. */
	fun init()
	{
		event.register(handler)
	}
}