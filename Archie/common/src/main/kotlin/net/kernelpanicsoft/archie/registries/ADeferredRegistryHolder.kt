package net.kernelpanicsoft.archie.registries

import dev.architectury.platform.Mod
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.kernelpanicsoft.archie.util.onClient
import net.kernelpanicsoft.archie.util.rem
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import kotlin.reflect.KProperty

/**
 * A registry holder that stores every registered entry in a [Map] keyed by its [ResourceLocation],
 * providing O(1) lookup by id string as well as property delegation via `by register(...)`.
 *
 * This class wraps an Architectury [DeferredRegister] and exposes the registered suppliers
 * through the [Map] interface. Use it as a base for per-registry object singletons.
 *
 * **Note:** Always use `by register(...)` (delegation) rather than calling `.get()` eagerly,
 * to avoid touching the registry before it is unfrozen.
 *
 * ### Example
 * ```kotlin
 * object MyItems : ADeferredRegistryHolder<Item>(MyMod.MOD, Registries.ITEM) {
 *     val MY_ITEM by register("my_item") { Item(Item.Properties()) }
 * }
 * // In mod init:
 * MyItems.init()
 * ```
 *
 * @param T The registry entry type.
 */
abstract class ADeferredRegistryHolder<T> private constructor(
	private val mod: Mod,
	registryKey: ResourceKey<Registry<T>>,
	private val map: MutableMap<ResourceLocation, RegistrySupplier<out T>>
) :
	Map<ResourceLocation, RegistrySupplier<out T>> by map
{
	constructor(mod: Mod, registryKey: ResourceKey<Registry<T>>) : this(mod, registryKey, mutableMapOf())

	private val registry: DeferredRegister<T> = DeferredRegister.create(mod.modId, registryKey)

	/**
	 * Registers the underlying [DeferredRegister], then schedules [initClient] to run on the
	 * client, at the earliest point registration APIs that depend on registries already being
	 * populated (e.g. Architectury's `MenuRegistry.registerScreenFactory`) are safe to call - see
	 * [scheduleEarlyClientRegistration]. Must be called once during mod initialization.
	 */
	fun init()
	{
		registry.register()
		onClient {
			scheduleEarlyClientRegistration(mod) {
				initClient()
			}
		}
	}

	/** Client-only setup run after [init] - see [scheduleEarlyClientRegistration] for exactly when. No-op by default. */
	open fun initClient() = Unit

	/** Looks up a registered entry by its unqualified [id] (namespaced under [mod] automatically). */
	operator fun get(id: String): RegistrySupplier<out T>? = map[mod % id]


	/** Property delegate operator that unwraps a [RegistrySupplier] to its concrete value. */
	operator fun <R : T> RegistrySupplier<R>.getValue(any: Any?, property: KProperty<*>): R
	{
		return get()
	}

	/** Registers an entry under [id] (namespaced under [mod]) and records it in [map]. */
	protected fun <R : T> register(id: String, supplier: () -> R): RegistrySupplier<R>
	{
		val ret = registry.register(id, supplier)
		map[ret.registryId] = ret
		return ret
	}

	/** Registers an entry under the fully-qualified [id] and records it in [map]. */
	protected fun <R : T> register(id: ResourceLocation, supplier: () -> R): RegistrySupplier<R>
	{
		val ret = registry.register(id, supplier)
		map[ret.registryId] = ret
		return ret
	}
}