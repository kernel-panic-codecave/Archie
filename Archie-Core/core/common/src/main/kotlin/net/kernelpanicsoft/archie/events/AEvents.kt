package net.kernelpanicsoft.archie.events

import net.kernelpanicsoft.archie.data.ADataGenerator
import net.kernelpanicsoft.archie.gametest.AGameTestPlatform
import dev.architectury.event.Event
import dev.architectury.event.EventFactory
import dev.architectury.event.EventResult
import dev.architectury.platform.Mod
import dev.architectury.platform.Platform
import dev.architectury.utils.Env
import net.kernelpanicsoft.archie.gametest.AGameTestSide

/**
 * Archie's central, mod-scoped event registry, built on top of Architectury's event system.
 *
 * A downstream mod opts in with `AEvents += MOD` (its own [Mod] descriptor), then listens for
 * [GATHER_DATA] and/or [REGISTER_GAME_TEST] via the corresponding handler's
 * [HandlerConstructor.create]. Handlers are mod-scoped: [GatherDataHandler] and
 * [RegisterGameTestHandler] both check the invoking [Mod] and no-op (via
 * [EventResult.pass]) for any mod other than the one they were created for.
 */
object AEvents
{
	/** Fired during datagen runs; handlers should gate by owning [Mod]. */
	val GATHER_DATA: Event<GatherDataHandler> = EventFactory.createEventResult()

	/** Fired during gametest registration runs; handlers should register test classes per [Mod]. */
	val REGISTER_GAME_TEST: Event<RegisterGameTestHandler> = EventFactory.createEventResult()

	private val mods: MutableList<Mod> = mutableListOf()

	/** Mods that opted into Archie event plumbing via `AEvents += MOD`. */
	val MODS: List<Mod>
		get() = mods

	fun register(mod: Mod)
	{
		mods.add(mod)
	}

	operator fun plusAssign(mod: Mod) = register(mod)

	/** Marker for a mod-scoped Architectury event listener created by a [HandlerConstructor]. */
	interface Handler<T>

	/** Builds a mod-scoped [H] whose body invokes `block` on the event's [T] payload. */
	fun interface HandlerConstructor<T, H : Handler<T>>
	{
		/** Creates an [H] for [mod] that runs [block] against the [T] payload when invoked. */
		fun create(mod: Mod, block: T.() -> Unit): H
	}

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

	/**
	 * DSL receiver passed to [REGISTER_GAME_TEST] listeners for declaring gametest classes.
	 *
	 * Classes registered via [server]/[client] are only collected on the matching
	 * [AGameTestPlatform] [AGameTestSide], unless [all] is `true`; classes registered via
	 * [common] are always collected. Collected classes are handed off to
	 * [AGameTestPlatform.register].
	 *
	 * @param all When `true`, [server] and [client] blocks are collected on both sides.
	 */
	class ArchieGameTestBuilder(private val all: Boolean = false)
	{
		/** All classes collected so far across [server], [client], and [common] blocks. */
		val classes: MutableList<Class<*>> = mutableListOf()

		/** Declares gametest classes that should only be registered on the server side. */
		fun server(block: Environment.Server.() -> Unit)
		{
			Environment.Server(all).apply(block).also { classes.addAll(it.classes) }
		}

		/** Declares gametest classes that should only be registered on the client side. */
		fun client(block: Environment.Client.() -> Unit)
		{
			Environment.Client(all).apply(block).also { classes.addAll(it.classes) }
		}

		/** Declares gametest classes that should always be registered, regardless of side. */
		fun common(block: Environment.Common.() -> Unit)
		{
			Environment.Common().apply(block).also { classes.addAll(it.classes) }
		}

		/** Scopes [register] calls to classes that should be collected only when [predicate] holds. */
		sealed class Environment(private val predicate: () -> Boolean)
		{
			/** Classes registered in this environment scope. */
			val classes: MutableList<Class<*>> = mutableListOf()
			class Server(all: Boolean) : Environment({ all || AGameTestPlatform.side == AGameTestSide.SERVER })
			class Client(all: Boolean) : Environment({ all || AGameTestPlatform.side == AGameTestSide.CLIENT })
			class Common : Environment({ true })

			/** Adds [clazz] to [classes] if this environment's [predicate] currently holds. */
			fun <T> register(clazz: Class<T>)
			{
				if (!predicate()) return
				classes.add(clazz)
			}

			/** Reified convenience for [register] using [T]'s [Class]. */
			inline fun <reified T> register()
			{
				register(T::class.java)
			}
		}
	}

	/**
	 * Handler for [REGISTER_GAME_TEST]. Implementations are produced via
	 * [HandlerConstructor.create] and forward to the registered `block` only when the firing
	 * [mod] matches the [Mod] the handler was created for, collecting declared test classes via
	 * an [ArchieGameTestBuilder] and registering each with [AGameTestPlatform.register].
	 */
	interface RegisterGameTestHandler : Handler<ArchieGameTestBuilder>
	{
		operator fun invoke(mod: Mod): EventResult

		companion object : HandlerConstructor<ArchieGameTestBuilder, RegisterGameTestHandler>
		{
			override fun create(
				mod: Mod,
				block: ArchieGameTestBuilder.() -> Unit
			): RegisterGameTestHandler
			{
				return RegisterGameTestHandlerImpl(mod, block)
			}

			class RegisterGameTestHandlerImpl internal constructor(
				private val mod: Mod,
				private val registerGameTests: ArchieGameTestBuilder.() -> Unit
			) : RegisterGameTestHandler
			{
				override operator fun invoke(mod: Mod): EventResult
				{
					if (this.mod != mod)
						return EventResult.pass()

					ArchieGameTestBuilder().apply(registerGameTests).classes.forEach { clazz ->
						AGameTestPlatform.register(clazz, mod)
					}
					return EventResult.interruptDefault()
				}


			}
		}
	}


}
