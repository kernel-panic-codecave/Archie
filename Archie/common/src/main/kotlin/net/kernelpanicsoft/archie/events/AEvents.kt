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
	
	interface Handler<T>

	fun interface HandlerConstructor<T, H : Handler<T>>
	{
		fun create(mod: Mod, block: T.() -> Unit): H
	}

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

	class ArchieGameTestBuilder(private val all: Boolean = false)
	{
		val classes: MutableList<Class<*>> = mutableListOf()
		fun server(block: Environment.Server.() -> Unit)
		{
			Environment.Server(all).apply(block).also { classes.addAll(it.classes) }
		}

		fun client(block: Environment.Client.() -> Unit)
		{
			Environment.Client(all).apply(block).also { classes.addAll(it.classes) }
		}

		fun common(block: Environment.Common.() -> Unit)
		{
			Environment.Common().apply(block).also { classes.addAll(it.classes) }
		}

		sealed class Environment(private val predicate: () -> Boolean)
		{
			val classes: MutableList<Class<*>> = mutableListOf()
			class Server(all: Boolean) : Environment({ all || AGameTestPlatform.side == AGameTestSide.SERVER })
			class Client(all: Boolean) : Environment({ all || AGameTestPlatform.side == AGameTestSide.CLIENT })
			class Common : Environment({ true })

			fun <T> register(clazz: Class<T>)
			{
				if (!predicate()) return
				classes.add(clazz)
			}

			inline fun <reified T> register()
			{
				register(T::class.java)
			}
		}
	}

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
