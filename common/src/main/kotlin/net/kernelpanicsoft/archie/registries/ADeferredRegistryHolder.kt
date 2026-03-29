package net.kernelpanicsoft.archie.registries

import dev.architectury.platform.Mod
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
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

	fun init()
	{
		registry.register()
	}

	operator fun get(id: String): RegistrySupplier<out T>? = map[ResourceLocation.fromNamespaceAndPath(mod.modId, id)]


	operator fun <R : T> RegistrySupplier<R>.getValue(any: Any?, property: KProperty<*>): R
	{
		return get()
	}

	protected fun <R : T> register(id: String, supplier: () -> R): RegistrySupplier<R>
	{
		val ret = registry.register(id, supplier)
		map[ret.registryId] = ret
		return ret
	}

	protected fun <R : T> register(id: ResourceLocation, supplier: () -> R): RegistrySupplier<R>
	{
		val ret = registry.register(id, supplier)
		map[ret.registryId] = ret
		return ret
	}
}