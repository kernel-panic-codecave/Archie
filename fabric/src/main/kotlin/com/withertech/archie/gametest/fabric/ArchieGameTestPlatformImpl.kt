package com.withertech.archie.gametest.fabric

import com.llamalad7.mixinextras.sugar.ref.LocalRef
import com.withertech.archie.events.ArchieEvents
import com.withertech.archie.gametest.ArchieGameTestPlatform
import dev.architectury.platform.Mod
import net.fabricmc.fabric.impl.gametest.FabricGameTestHelper
import net.fabricmc.fabric.impl.gametest.FabricGameTestModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import net.fabricmc.loader.api.entrypoint.EntrypointContainer
import net.minecraft.gametest.framework.GameTestRegistry
import kotlin.reflect.full.primaryConstructor

@Suppress("unused")
object ArchieGameTestPlatformImpl
{
	@Suppress("UnstableApiUsage")
	@JvmStatic
	val isGameTest: Boolean
		get() = FabricGameTestHelper.ENABLED

	@JvmStatic
	fun register(clazz: Class<*>, mod: Mod)
	{
		testClasses.getOrPut(mod, ::mutableListOf).add(clazz)
	}

	@JvmField
	internal val testClasses: MutableMap<Mod, MutableList<Class<*>>> = mutableMapOf()

	@JvmStatic
	@JvmName("addEntrypoints")
	internal fun addEntrypoints(entrypointContainers: LocalRef<MutableList<EntrypointContainer<Any?>>>)
	{
		if (ArchieGameTestPlatform.isGameTest)
		{
			val result: MutableList<EntrypointContainer<Any?>> = entrypointContainers.get().toMutableList()
			for (mod in ArchieEvents.MODS)
			{
				ArchieEvents.REGISTER_GAME_TEST.invoker()(mod)
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