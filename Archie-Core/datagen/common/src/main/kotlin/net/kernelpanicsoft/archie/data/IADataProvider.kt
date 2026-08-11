package net.kernelpanicsoft.archie.data

import dev.architectury.platform.Mod
import dev.architectury.platform.Platform
import net.minecraft.data.DataProvider
import net.minecraft.data.PackOutput
import net.minecraft.resources.ResourceLocation

/**
 * Common contract shared by Archie's [DataProvider] implementations, adding the current [mod]
 * and a couple of ID/path helpers used when generating output files.
 */
interface IADataProvider : DataProvider
{
	val output: PackOutput

	/** The mod this provider is generating data for. */
	val mod: Mod

	/** Whether datagen should abort with an error instead of logging and continuing. */
	val exitOnError: Boolean

	/** Builds a [ResourceLocation] in [mod]'s namespace, e.g. for output file paths. */
	fun modLoc(name: String): ResourceLocation
	{
		return ResourceLocation.fromNamespaceAndPath(mod.modId, name)
	}

	/** Builds a [ResourceLocation] in the `minecraft` namespace. */
	fun mcLoc(name: String): ResourceLocation
	{
		return ResourceLocation.withDefaultNamespace(name)
	}

	/**
	 * Formats a provider display [name] (used for `getName()`), prefixing it with [mod]'s name
	 * on loaders other than Fabric so providers from different mods are distinguishable in
	 * datagen logs.
	 */
	fun format(name: String): String = if (Platform.isFabric()) name else "${mod.name}/$name"
}