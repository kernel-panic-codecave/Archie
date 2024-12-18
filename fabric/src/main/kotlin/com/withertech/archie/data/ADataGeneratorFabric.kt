package com.withertech.archie.data

import dev.architectury.platform.Mod
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import net.minecraft.data.DataGenerator
import net.minecraft.data.DataProvider

class ADataGeneratorFabric(private val fabricDataGenerator: FabricDataGenerator, override val mod: Mod) : ADataGenerator()
{
	override fun <T : DataProvider> addProvider(run: Boolean, factory: ARegistryAwareDataProviderFactory<T>): T
	{
		val pack = fabricDataGenerator.createPack()
		val toRun = DataGenerator.PackGenerator::class.java.getDeclaredField("toRun")
		toRun.isAccessible = true
		toRun.setBoolean(pack, run)
		return pack.addProvider { output, registriesFuture ->
			factory(output, registriesFuture)
		}
	}
}