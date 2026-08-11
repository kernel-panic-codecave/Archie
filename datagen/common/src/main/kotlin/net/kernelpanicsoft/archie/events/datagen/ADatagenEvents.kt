package net.kernelpanicsoft.archie.events.datagen

import dev.architectury.event.Event
import dev.architectury.event.EventFactory
import dev.architectury.event.EventResult
import dev.architectury.platform.Mod
import net.kernelpanicsoft.archie.data.ADataGenerator
import net.kernelpanicsoft.archie.events.base.Handler
import net.kernelpanicsoft.archie.events.base.HandlerConstructor

/**
 * `archie-datagen`'s central, mod-scoped event registry, built on top of Architectury's event
 * system - the datagen half of what used to be `archie-core`'s combined `AEvents`.
 *
 * A downstream mod opts in with `ADatagenEvents += MOD` (its own [Mod] descriptor), then listens
 * for [GATHER_DATA] via [GatherDataHandler.Companion.create]. Handlers are mod-scoped: they check
 * the invoking [Mod] and no-op (via [EventResult.pass]) for any mod other than the one they were
 * created for.
 */
object ADatagenEvents
{
	/** Fired during datagen runs; handlers should gate by owning [Mod]. */
	val GATHER_DATA: Event<GatherDataHandler> = EventFactory.createEventResult()

	private val mods: MutableList<Mod> = mutableListOf()

	/** Mods that opted into `archie-datagen`'s event plumbing via `ADatagenEvents += MOD`. */
	val MODS: List<Mod>
		get() = mods

	fun register(mod: Mod)
	{
		mods.add(mod)
	}

	operator fun plusAssign(mod: Mod) = register(mod)

	/**
	 * Handler for [GATHER_DATA]. Implementations are produced via [HandlerConstructor.create]
	 * and forward to the registered `block` only when the firing [ADataGenerator.mod] matches
	 * the [Mod] the handler was created for.
	 */
	interface GatherDataHandler : Handler<ADataGenerator>
	{
		operator fun invoke(dataGenerator: ADataGenerator): EventResult

		companion object : HandlerConstructor<ADataGenerator, GatherDataHandler>
		{
			override fun create(mod: Mod, block: ADataGenerator.() -> Unit): GatherDataHandler
			{
				return GatherDataHandlerImpl(mod, block)
			}

			class GatherDataHandlerImpl internal constructor(
				private val mod: Mod,
				private val gatherData: ADataGenerator.() -> Unit
			) :
				GatherDataHandler
			{
				override operator fun invoke(dataGenerator: ADataGenerator): EventResult
				{
					if (this.mod != dataGenerator.mod)
						return EventResult.pass()
					dataGenerator.gatherData()
					return EventResult.interruptDefault()
				}
			}
		}
	}
}
