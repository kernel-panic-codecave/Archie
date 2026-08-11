package net.kernelpanicsoft.archie.gametest

import net.kernelpanicsoft.archie.events.AGametestEvents
import net.kernelpanicsoft.archie.events.AEventObject
import dev.architectury.event.Event
import dev.architectury.platform.Mod

/**
 * Convenience [AEventObject] base for listening to [AGametestEvents.REGISTER_GAME_TEST] for
 * [mod]. Subclass and override [handler] (an [AGametestEvents.ArchieGameTestBuilder] receiver)
 * to declare gametest classes via `server { register<...>() }` / `client { ... }` / `common { ... }`.
 */
abstract class AGameTestEventObject(mod: Mod) :
	AEventObject<AGametestEvents.ArchieGameTestBuilder, AGametestEvents.RegisterGameTestHandler, AGametestEvents.RegisterGameTestHandler.Companion>(
		mod
	)
{
	override val event: Event<AGametestEvents.RegisterGameTestHandler> = AGametestEvents.REGISTER_GAME_TEST
	override val handlerConstructor: AGametestEvents.RegisterGameTestHandler.Companion = AGametestEvents.RegisterGameTestHandler.Companion
}
