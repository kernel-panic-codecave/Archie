package com.withertech.archie.data

import dev.architectury.platform.Mod
import net.minecraft.data.DataProvider
import net.minecraft.data.PackOutput
import net.neoforged.neoforge.data.event.GatherDataEvent

class ArchieDataGeneratorNeoForge(private val forgeDataGenerator: GatherDataEvent, override val mod: Mod) : ArchieDataGenerator()
{
	override fun <T : DataProvider> addProvider(run: Boolean, factory: RegistryAwareArchieDataProviderFactory<T>): T
	{
		return forgeDataGenerator.generator.addProvider(run, DataProvider.Factory { output: PackOutput ->
			factory(output, forgeDataGenerator.lookupProvider)
		})
	}
}