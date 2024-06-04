package com.withertech.archie.events

import com.withertech.archie.data.ArchieDataGenerator
import com.withertech.archie.gametest.ArchieGameTestPlatform
import dev.architectury.event.Event
import dev.architectury.event.EventFactory
import dev.architectury.event.EventResult
import dev.architectury.platform.Mod

object ArchieEvents
{
	val GATHER_DATA: Event<GatherDataHandler> = EventFactory.createEventResult()

	val REGISTER_GAME_TEST: Event<RegisterGameTestHandler> = EventFactory.createEventResult()

	val MODS: List<Mod> = mutableListOf()

	fun register(mod: Mod) = (MODS as MutableList<Mod>).add(mod).let {  }

	operator fun plusAssign(mod: Mod) = register(mod)

	interface GatherDataHandler
	{
		operator fun invoke(dataGenerator: ArchieDataGenerator): EventResult

		companion object
		{
			fun create(mod: Mod, gatherData: ArchieDataGenerator.() -> Unit): GatherDataHandler
			{
				return GatherDataHandlerImpl(mod, gatherData)
			}

			class GatherDataHandlerImpl internal constructor(
				private val mod: Mod,
				private val gatherData: ArchieDataGenerator.() -> Unit
			) :
				GatherDataHandler
			{
				override operator fun invoke(dataGenerator: ArchieDataGenerator): EventResult
				{
					if (this.mod != dataGenerator.mod)
						return EventResult.pass()
					dataGenerator.gatherData()
					return EventResult.interruptDefault()
				}
			}
		}
	}

	interface RegisterGameTestHandler
	{
		operator fun invoke(mod: Mod): EventResult

		companion object
		{
			fun create(
				mod: Mod,
				registerGameTests: RegisterGameTestHandlerImpl.ArchieGameTestBuilder.() -> Unit
			): RegisterGameTestHandler
			{
				return RegisterGameTestHandlerImpl(mod, registerGameTests)
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

					mod(this.mod, registerGameTests)
					return EventResult.interruptDefault()
				}

				internal fun mod(mod: Mod, block: ArchieGameTestBuilder.() -> Unit)
				{
					ArchieGameTestBuilder(mod).block()
				}

				data class ArchieGameTestBuilder internal constructor(
					private val mod: Mod
				)
				{
					fun <T> register(clazz: Class<T>)
					{
						ArchieGameTestPlatform.register(clazz, mod)
					}

					inline fun <reified T> register()
					{
						register(T::class.java)
					}
				}
			}
		}
	}


}
