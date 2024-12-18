package com.withertech.archie.registries

import dev.architectury.platform.Mod
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import kotlin.reflect.KProperty

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