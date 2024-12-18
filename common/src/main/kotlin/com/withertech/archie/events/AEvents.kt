package com.withertech.archie.events

import com.withertech.archie.data.ADataGenerator
import com.withertech.archie.gametest.AGameTestPlatform
import dev.architectury.event.Event
import dev.architectury.event.EventFactory
import dev.architectury.event.EventResult
import dev.architectury.platform.Mod

object AEvents
{
	val GATHER_DATA: Event<GatherDataHandler> = EventFactory.createEventResult()

	val REGISTER_GAME_TEST: Event<RegisterGameTestHandler> = EventFactory.createEventResult()

	val MODS: List<Mod> = mutableListOf()

	fun register(mod: Mod) = (MODS as MutableList<Mod>).add(mod).let {  }

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

	data class ArchieGameTestBuilder internal constructor(
		private val mod: Mod
	)
	{
		fun <T> register(clazz: Class<T>)
		{
			AGameTestPlatform.register(clazz, mod)
		}

		inline fun <reified T> register()
		{
			register(T::class.java)
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

					ArchieGameTestBuilder(mod).registerGameTests()

					return EventResult.interruptDefault()
				}


			}
		}
	}


}
