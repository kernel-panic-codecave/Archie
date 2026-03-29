package net.kernelpanicsoft.archie.util

import net.kernelpanicsoft.archie.Archie
import net.minecraft.resources.ResourceLocation

operator fun String.rem(other: String) = ResourceLocation.fromNamespaceAndPath(this, other)
operator fun Archie.rem(other: String) = ResourceLocation.fromNamespaceAndPath(MOD_ID, other)

operator fun ResourceLocation.div(other: String) = withSuffix("/$other")
operator fun ResourceLocation.div(other: ResourceLocation) = withSuffix("/${other.path}")
operator fun String.div(other: ResourceLocation) = other.withPrefix("$this/")