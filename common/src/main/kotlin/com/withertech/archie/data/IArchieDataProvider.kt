package com.withertech.archie.data

import dev.architectury.platform.Mod
import dev.architectury.platform.Platform
import net.minecraft.data.DataProvider
import net.minecraft.data.PackOutput
import net.minecraft.resources.ResourceLocation

interface IArchieDataProvider : DataProvider
{
	val output: PackOutput

	val mod: Mod

	val exitOnError: Boolean

	fun modLoc(name: String): ResourceLocation
	{
		return ResourceLocation(mod.modId, name)
	}

	fun mcLoc(name: String): ResourceLocation
	{
		return ResourceLocation(name)
	}

	fun format(name: String): String = if (Platform.isFabric()) name else "${mod.name}/$name"
}