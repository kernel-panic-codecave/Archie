package com.withertech.archie.registries

import dev.architectury.extensions.injected.InjectedRegistryEntryExtension
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.Holder
import net.minecraft.resources.ResourceLocation
import kotlin.reflect.KProperty

val <T> InjectedRegistryEntryExtension<T>.id: ResourceLocation
	get() = `arch$registryName`()!!

val <T> InjectedRegistryEntryExtension<T>.holder: Holder<T>
	get() = `arch$holder`()

