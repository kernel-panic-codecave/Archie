package net.kernelpanicsoft.archie.gametest

import com.llamalad7.mixinextras.sugar.ref.LocalRef
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import net.fabricmc.loader.api.entrypoint.EntrypointContainer
import net.kernelpanicsoft.archie.events.AEvents
import net.kernelpanicsoft.archie.gametest.AGameTestPlatform.isGameTest
import net.kernelpanicsoft.archie.gametest.AGameTestPlatform.testClasses
import kotlin.reflect.full.primaryConstructor

internal object AGameTestPlatformInternal
{
	@JvmStatic
	@JvmName("addEntrypoints")
	internal fun addEntrypoints(entrypointContainers: LocalRef<MutableList<EntrypointContainer<Any?>>>)
	{
		if (isGameTest)
		{
			val result: MutableList<EntrypointContainer<Any?>> = entrypointContainers.get().toMutableList()
			for (mod in AEvents.MODS)
			{
				AEvents.REGISTER_GAME_TEST.invoker()(mod)
				for (clazz in testClasses.getOrPut(mod, ::mutableListOf))
				{
					result.add(object : EntrypointContainer<Any?>
					{
						override fun getEntrypoint(): Any?
						{
							return clazz.kotlin.objectInstance ?: clazz.kotlin.primaryConstructor?.call()
						}

						override fun getProvider(): ModContainer
						{
							return FabricLoader.getInstance().getModContainer(mod.modId).orElse(null)
						}
					})
				}
			}
			entrypointContainers.set(result)
		}
	}
}