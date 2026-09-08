package net.kernelpanicsoft.archie.registries

import dev.architectury.platform.Mod
import dev.architectury.platform.hooks.EventBusesHooks
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.neoforged.bus.api.EventPriority
import net.neoforged.neoforge.registries.RegisterEvent

/**
 * Client-only registration APIs that touch a `by register(...)` entry (e.g. passing a `MenuType`
 * to `MenuRegistry.registerScreenFactory`, or a `BlockEntityType` to a renderer registration)
 * aren't safe to call before [registryKey]'s registry has actually been populated, which for a
 * [Mod]-scoped [dev.architectury.registry.registries.DeferredRegister] happens on NeoForge's
 * [RegisterEvent] - not at [ADeferredRegistryHolder.init]'s own call time. Some of those APIs
 * (`MenuRegistry.registerScreenFactory` among them) also only subscribe a listener at call time -
 * their actual effect happens later, on a *different* event fired on Architectury's own per-mod
 * bus, not [mod]'s - so deferring [block] to [mod]'s own firing of that same event (this platform
 * used to hook `RegisterMenuScreensEvent` directly) races Architectury's: if Architectury's bus
 * fires first, the listener `block` adds is added too late for that one-shot event and never runs.
 *
 * [RegisterEvent] is fired once per mod, per registry, before any client registration-stage event
 * (entries must exist before, say, their screens or renderers can be registered) - so hooking
 * [mod]'s own firing of it, filtered to [registryKey], satisfies both constraints: late enough
 * that this holder's own entries are already populated, early enough that whatever listener
 * [block] itself adds (on Architectury's bus, in the `MenuRegistry.registerScreenFactory` case)
 * is in place well before any mod's later registration-stage events fire. [EventPriority.LOWEST]
 * makes the ordering explicit rather than relying on this listener happening to be added after
 * the [dev.architectury.registry.registries.DeferredRegister]'s own [RegisterEvent] listener
 * (which does the actual population) - it runs after every other listener for this firing, on
 * any priority, has already run.
 */
actual fun waitForRegistry(mod: Mod, registryKey: ResourceKey<out Registry<*>>, block: () -> Unit)
{
	EventBusesHooks.whenAvailable(mod.modId) { bus ->
		bus.addListener(EventPriority.LOWEST, RegisterEvent::class.java) { event ->
			if (event.registryKey == registryKey) block()
		}
	}
}
