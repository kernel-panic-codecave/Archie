package com.withertech.archie.data

import com.llamalad7.mixinextras.sugar.ref.LocalRef
import com.withertech.archie.events.AEvents
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import net.fabricmc.loader.api.entrypoint.EntrypointContainer
import net.minecraft.core.RegistrySetBuilder

@Suppress("unused")
actual object ADataGeneratorPlatform
{
	actual val isDataGen: Boolean
		get() = System.getProperty("archie.datagen").toBoolean()

	@JvmStatic
	@JvmName("addEntrypoints")
	internal fun addEntrypoints(dataGeneratorInitializers: LocalRef<MutableList<EntrypointContainer<DataGeneratorEntrypoint>>>)
	{
		if (isDataGen)
		{
			val result: MutableList<EntrypointContainer<DataGeneratorEntrypoint>> = dataGeneratorInitializers.get().toMutableList()
			for (mod in AEvents.MODS)
			{
				result.add(object : EntrypointContainer<DataGeneratorEntrypoint>
				{
					override fun getEntrypoint(): DataGeneratorEntrypoint
					{
						return object : DataGeneratorEntrypoint
						{
							override fun onInitializeDataGenerator(fabricDataGenerator: FabricDataGenerator)
							{
								AEvents.GATHER_DATA.invoker()(ADataGeneratorFabric(fabricDataGenerator, mod))
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