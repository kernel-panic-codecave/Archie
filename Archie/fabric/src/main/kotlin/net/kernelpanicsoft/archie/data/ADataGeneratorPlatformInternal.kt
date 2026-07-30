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

/**
 * Backs `FabricDataGenHelperMixin`, which calls [addEntrypoints] mid-way through
 * `FabricDataGenHelper.runInternal()` to splice in synthetic datagen entrypoints.
 *
 * Archie mods don't declare a `fabric-datagen` entrypoint in `fabric.mod.json`; instead they
 * register with [AEvents.MODS] at init time. This object bridges that registration into the
 * `EntrypointContainer<DataGeneratorEntrypoint>` list Fabric's data generator actually consumes.
 */
internal object ADataGeneratorPlatformInternal
{
	/**
	 * Appends one [EntrypointContainer] per mod in [AEvents.MODS] to [dataGeneratorInitializers],
	 * each of which fires [AEvents.GATHER_DATA] with an [ADataGeneratorFabric] for that mod. No-op
	 * outside a datagen run ([ADataGeneratorPlatform.isDataGen] false).
	 */
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