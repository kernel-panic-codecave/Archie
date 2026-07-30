package net.kernelpanicsoft.archie.registries

import dev.architectury.extensions.injected.InjectedRegistryEntryExtension
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.Holder
import net.minecraft.resources.ResourceLocation
import kotlin.reflect.KProperty

/** The registry name Architectury injected into this registry entry. Only valid once registered. */
val <T> InjectedRegistryEntryExtension<T>.id: ResourceLocation
	get() = `arch$registryName`()!!

/** The [Holder] Architectury injected into this registry entry. Only valid once registered. */
val <T> InjectedRegistryEntryExtension<T>.holder: Holder<T>
	get() = `arch$holder`()

