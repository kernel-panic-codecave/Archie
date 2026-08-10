package net.kernelpanicsoft.archie.registries

import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.resources.ResourceLocation
import kotlin.reflect.KProperty

/**
 * A convenience base class for managing a collection of registry entries backed by an
 * Architectury [DeferredRegister].
 *
 * Subclass this object (or class) for each registry you want to populate, declare your
 * entries with `by register(...)`, and call [init] during your mod's initialization phase.
 *
 * **Important:** Always use the `by` delegation operator when declaring entries to avoid
 * "Registry is frozen" errors that occur when values are accessed before the registry tick.
 *
 * ### Example
 * ```kotlin
 * object MyItems : RegistryHelper<Item>(DeferredRegister.create(modId, Registries.ITEM)) {
 *     val MY_ITEM by register("my_item") { Item(Item.Properties()) }
 * }
 *
 * // In mod init:
 * MyItems.init()
 * ```
 *
 * @param T The base type stored in the target registry.
 * @property registry The [DeferredRegister] to which entries will be submitted.
 */
abstract class RegistryHelper<T : Any>(val registry: DeferredRegister<T>) {

    /**
     * Registers this helper's [DeferredRegister] with the game's registry system.
     *
     * Must be called once during mod initialization.
     */
    open fun init() = registry.register()

    /**
     * Registers a new entry and returns a [RegistrySupplier] for lazy access.
     *
     * @param id The entry's registry name (without namespace). The mod namespace is prepended automatically.
     * @param supplier Factory producing the entry. Must not cache the returned instance.
     * @return A [RegistrySupplier] wrapping the registered entry.
     */
    open fun <V : T> register(id: String, supplier: () -> V): RegistrySupplier<V> =
        registry.register(id, supplier)

    /**
     * Registers a new entry using a fully-qualified [ResourceLocation] and returns a [RegistrySupplier].
     *
     * @param id The fully-qualified [ResourceLocation] for the entry.
     * @param supplier Factory producing the entry. Must not cache the returned instance.
     * @return A [RegistrySupplier] wrapping the registered entry.
     */
    open fun <V : T> register(id: ResourceLocation, supplier: () -> V): RegistrySupplier<V> =
        registry.register(id, supplier)

    /**
     * Kotlin property delegate operator that unwraps a [RegistrySupplier] to its concrete value.
     *
     * This enables the idiomatic `val MY_ENTRY by register(...)` pattern.
     */
    operator fun <V : T> RegistrySupplier<V>.getValue(any: Any?, property: KProperty<*>): V = get()
}
