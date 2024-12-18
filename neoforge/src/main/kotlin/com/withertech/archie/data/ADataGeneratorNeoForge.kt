package com.withertech.archie.data

import dev.architectury.platform.Mod
import net.minecraft.data.DataProvider
import net.minecraft.data.PackOutput
import net.neoforged.neoforge.data.event.GatherDataEvent

class ADataGeneratorNeoForge(private val forgeDataGenerator: GatherDataEvent, override val mod: Mod) : ADataGenerator()
{
	override fun <T : DataProvider> addProvider(run: Boolean, factory: ARegistryAwareDataProviderFactory<T>): T
	{
		return forgeDataGenerator.generator.addProvider(run, DataProvider.Factory { output: PackOutput ->
			factory(output, forgeDataGenerator.lookupProvider)
		})
	}
}