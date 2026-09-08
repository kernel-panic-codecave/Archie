package net.kernelpanicsoft.archie.registries

import dev.architectury.platform.Mod
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey

/**
 * Schedules [block] to run on the client at the earliest point client-side registration APIs
 * that depend on [registryKey]'s registry already being populated - like Architectury's
 * `MenuRegistry.registerScreenFactory`, or registering a block-entity renderer for a just-created
 * `BlockEntityType` - are safe to call. Backed by an `actual` per mod loader, since the loaders
 * genuinely differ on where that point is; only call this from inside a client-only guard (e.g.
 * [net.kernelpanicsoft.archie.util.onClient]) - it does no environment checking of its own.
 *
 * [block] isn't necessarily menu-screen-specific - [ADeferredRegistryHolder.initClient] is a
 * general client-setup hook any registry holder (blocks, items, block entity types, menu types,
 * ...) can override for whatever client-only registration its own entries need, which is why this
 * takes [registryKey] rather than assuming a single fixed registry.
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
 * @param registryKey The registry this holder's own entries belong to - NeoForge's `actual` waits
 *   specifically for *this* registry's population event before running [block].
 */
expect fun waitForRegistry(mod: Mod, registryKey: ResourceKey<out Registry<*>>, block: () -> Unit)
