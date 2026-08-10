package net.kernelpanicsoft.archie.events

import dev.architectury.event.Event
import dev.architectury.platform.Mod

/**
 * Base class for Archie's mod-scoped Architectury event wrappers, such as
 * [AEvents.GatherDataHandler] and [AEvents.RegisterGameTestHandler].
 *
 * Subclasses wire together an Architectury [event], the [handlerConstructor] that builds a
 * [mod]-scoped [H] from a [T] callback, and the [handler] logic itself, then call [init] once
 * (idempotently, thread-safely) to register with the underlying event.
 *
 * @param T The event payload/receiver type passed to [handler].
 * @param H The [AEvents.Handler] type produced for [mod].
 * @param C The [AEvents.HandlerConstructor] that builds an [H].
 * @param mod The [Mod] this event object is scoped to.
 */
abstract class AEventObject<T, H : AEvents.Handler<T>, C : AEvents.HandlerConstructor<T, H>>(val mod: Mod)
{
	/** The underlying Architectury event this wrapper registers a handler with. */
	abstract val event: Event<H>

	@Volatile
	private var initialized: Boolean = false

	/** Builds a [mod]-scoped [H] from the [handler] callback. */
	abstract val handlerConstructor: C

	/** The callback invoked when [event] fires for [mod]. */
	abstract fun T.handler()

	/**
	 * Registers [handler] with [event] via [handlerConstructor]. Safe to call multiple times;
	 * only the first call has any effect.
	 */
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