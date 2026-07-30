package net.kernelpanicsoft.archie.data

import dev.architectury.platform.Mod
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import net.minecraft.data.DataGenerator
import net.minecraft.data.DataProvider

/** Fabric [ADataGenerator], registering providers with a Fabric [FabricDataGenerator] pack. */
class ADataGeneratorFabric(private val fabricDataGenerator: FabricDataGenerator, override val mod: Mod) : ADataGenerator()
{
	/** Reflectively forces the created provider's `toRun` flag since Fabric's pack API always runs a provider once added. */
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