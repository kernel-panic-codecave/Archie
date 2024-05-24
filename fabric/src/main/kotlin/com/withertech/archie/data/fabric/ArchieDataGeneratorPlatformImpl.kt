package com.withertech.archie.data.fabric

import com.llamalad7.mixinextras.sugar.ref.LocalRef
import com.withertech.archie.data.ArchieDataGenerator
import com.withertech.archie.data.ArchieDataGeneratorPlatform
import com.withertech.archie.events.ArchieEvents
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import net.fabricmc.loader.api.entrypoint.EntrypointContainer
import net.minecraft.core.RegistrySetBuilder

@Suppress("unused")
object ArchieDataGeneratorPlatformImpl
{
	@JvmStatic
	val isDataGen: Boolean
		get() = System.getProperty("archie.datagen").toBoolean()

	@JvmStatic
	@JvmName("addEntrypoints")
	internal fun addEntrypoints(dataGeneratorInitializers: LocalRef<MutableList<EntrypointContainer<DataGeneratorEntrypoint>>>)
	{
		if (ArchieDataGeneratorPlatform.isDataGen)
		{
			val result: MutableList<EntrypointContainer<DataGeneratorEntrypoint>> = dataGeneratorInitializers.get().toMutableList()
			for (mod in ArchieEvents.MODS)
			{
				result.add(object : EntrypointContainer<DataGeneratorEntrypoint>
				{
					override fun getEntrypoint(): DataGeneratorEntrypoint
					{
						return object : DataGeneratorEntrypoint
						{
							override fun onInitializeDataGenerator(fabricDataGenerator: FabricDataGenerator)
							{
								ArchieEvents.GATHER_DATA.invoker()(ArchieDataGeneratorFabric(fabricDataGenerator, mod))
							}

							override fun buildRegistry(registryBuilder: RegistrySetBuilder?)
							{
								super.buildRegistry(registryBuilder)
							}
						}
					}

					override fun getProvider(): ModContainer
					{
						return FabricLoader.getInstance().getModContainer(mod.modId).orElse(null)
					}

				})
			}
			dataGeneratorInitializers.set(result)
		}
	}
}