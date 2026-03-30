package net.kernelpanicsoft.archie.data

import com.llamalad7.mixinextras.sugar.ref.LocalRef
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import net.fabricmc.loader.api.entrypoint.EntrypointContainer
import net.kernelpanicsoft.archie.data.ADataGeneratorPlatform.isDataGen
import net.kernelpanicsoft.archie.events.AEvents
import net.minecraft.core.RegistrySetBuilder

internal object ADataGeneratorPlatformInternal
{
	@JvmStatic
	@JvmName("addEntrypoints")
	internal fun addEntrypoints(dataGeneratorInitializers: LocalRef<MutableList<EntrypointContainer<DataGeneratorEntrypoint>>>)
	{
		if (!isDataGen) return

		// Fabric expects datagen entrypoints; inject one per registered Archie mod.
		val result = dataGeneratorInitializers.get().toMutableList()
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