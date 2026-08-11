package net.kernelpanicsoft.archie.data

import net.kernelpanicsoft.archie.events.ADatagenEvents
import net.kernelpanicsoft.archie.events.ADatagenEvents.GatherDataHandler
import net.kernelpanicsoft.archie.events.AEventObject
import dev.architectury.event.Event
import dev.architectury.platform.Mod

/**
 * Convenience [AEventObject] base for hooking into [ADatagenEvents.GATHER_DATA], the event fired
 * by the loader during a datagen run. Implement [handler] to build and run an [ADataGenerator].
 */
abstract class ADatagenEventObject(mod: Mod) :
	AEventObject<ADataGenerator, GatherDataHandler, GatherDataHandler.Companion>(
		mod
	)
{
	override val event: Event<GatherDataHandler> = ADatagenEvents.GATHER_DATA
	override val handlerConstructor: GatherDataHandler.Companion = GatherDataHandler.Companion
}
