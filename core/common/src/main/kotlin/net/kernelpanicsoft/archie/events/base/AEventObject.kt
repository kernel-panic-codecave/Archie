package net.kernelpanicsoft.archie.events.base

import dev.architectury.event.Event
import dev.architectury.platform.Mod

/** Marker for a mod-scoped Architectury event listener created by a [HandlerConstructor]. */
interface Handler<T>

/** Builds a mod-scoped [H] whose body invokes `block` on the event's [T] payload. */
fun interface HandlerConstructor<T, H : Handler<T>>
{
	/** Creates an [H] for [mod] that runs [block] against the [T] payload when invoked. */
	fun create(mod: Mod, block: T.() -> Unit): H
}

/**
 * Base class for a mod-scoped Architectury event wrapper (e.g. `archie-datagen`'s
 * `GatherDataHandler`, `archie-gametest`'s `RegisterGameTestHandler`).
 *
 * Subclasses wire together an Architectury [event], the [handlerConstructor] that builds a
 * [mod]-scoped [H] from a [T] callback, and the [handler] logic itself, then call [init] once
 * (idempotently, thread-safely) to register with the underlying event.
 *
 * @param T The event payload/receiver type passed to [handler].
 * @param H The [Handler] type produced for [mod].
 * @param C The [HandlerConstructor] that builds an [H].
 * @param mod The [Mod] this event object is scoped to.
 */
abstract class AEventObject<T, H : Handler<T>, C : HandlerConstructor<T, H>>(val mod: Mod)
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
