package net.kernelpanicsoft.archie.registries

import dev.architectury.platform.Mod

/**
 * Schedules [block] to run on the client at the earliest point client-side registration APIs
 * that depend on registries already being populated - like Architectury's
 * `MenuRegistry.registerScreenFactory` - are safe to call. Backed by an `actual` per mod loader,
 * since the loaders genuinely differ on where that point is; only call this from inside a
 * client-only guard (e.g. [net.kernelpanicsoft.archie.util.onClient]) - it does no environment
 * checking of its own.
 *
 * On Fabric there's no staged registry-event model to race, so this runs [block] effectively
 * immediately. On NeoForge, [dev.architectury.event.events.common.LifecycleEvent.SETUP]/
 * `FMLCommonSetupEvent` - the timing [ADeferredRegistryHolder.initClient] used to schedule on
 * unconditionally - actually runs *after* several client registration-stage events (e.g.
 * `RegisterMenuScreensEvent`), so calling `MenuRegistry.registerScreenFactory` from there
 * silently never fires: it internally attaches a listener for that exact event, which has
 * already fired and moved on by the time Common Setup runs.
 *
 * @param mod The mod whose event bus [block] should run on (NeoForge only needs this - Fabric's
 *   `actual` ignores it, since Fabric has no per-mod bus to look up).
 */
expect fun scheduleEarlyClientRegistration(mod: Mod, block: () -> Unit)
