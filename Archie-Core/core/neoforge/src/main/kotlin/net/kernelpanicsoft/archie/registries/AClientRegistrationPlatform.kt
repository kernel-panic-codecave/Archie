package net.kernelpanicsoft.archie.registries

import dev.architectury.platform.Mod
import dev.architectury.platform.hooks.EventBusesHooks
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent

/**
 * [RegisterMenuScreensEvent] is the earliest point NeoForge's own client registration-stage
 * events fire, and specifically the one `MenuRegistry.registerScreenFactory` itself listens
 * for internally - hooking the exact same event here (on [mod]'s own bus, since it's a per-mod
 * event) guarantees this fires before that internal listener would otherwise miss it.
 */
actual fun scheduleEarlyClientRegistration(mod: Mod, block: () -> Unit)
{
	EventBusesHooks.whenAvailable(mod.modId) { bus ->
		bus.addListener(RegisterMenuScreensEvent::class.java) { block() }
	}
}
