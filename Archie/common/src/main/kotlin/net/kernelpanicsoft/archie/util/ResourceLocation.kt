package net.kernelpanicsoft.archie.util

import dev.architectury.platform.Mod
import net.kernelpanicsoft.archie.Archie
import net.minecraft.resources.ResourceLocation

/** Builds a [ResourceLocation] with `this` as the namespace and [other] as the path, e.g. `"mymod" % "my_item"`. */
operator fun String.rem(other: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(this, other)
/** Builds a [ResourceLocation] namespaced under this [Mod]'s id, with [other] as the path, e.g. `MyMod.MOD % "my_item"`. */
operator fun Mod.rem(other: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(this.modId, other)
/** Builds a [ResourceLocation] namespaced under [Archie.MOD_ID], with [other] as the path, e.g. `Archie % "main"`. */
operator fun Archie.rem(other: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, other)

/** Appends `/[other]` to this location's path. */
operator fun ResourceLocation.div(other: String): ResourceLocation = withSuffix("/$other")
/** Appends `/` plus [other]'s path (namespace of [other] is ignored) to this location's path. */
operator fun ResourceLocation.div(other: ResourceLocation): ResourceLocation = withSuffix("/${other.path}")
/** Prepends `this/` to [other]'s path, keeping [other]'s namespace. */
operator fun String.div(other: ResourceLocation): ResourceLocation = other.withPrefix("$this/")