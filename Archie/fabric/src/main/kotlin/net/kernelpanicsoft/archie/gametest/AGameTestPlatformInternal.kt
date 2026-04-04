package net.kernelpanicsoft.archie.gametest

import com.llamalad7.mixinextras.sugar.ref.LocalRef
import dev.architectury.platform.Mod
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import net.fabricmc.loader.api.entrypoint.EntrypointContainer
import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.events.AEvents
import net.kernelpanicsoft.archie.gametest.AGameTestPlatform.isGameTest
import kotlin.reflect.full.primaryConstructor

internal object AGameTestPlatformInternal
{

	@JvmField
	internal val testClasses: MutableMap<Mod, MutableSet<Class<*>>> = mutableMapOf()

	@JvmStatic
	@JvmName("addEntrypoints")
	internal fun addEntrypoints(entrypointContainers: LocalRef<MutableList<EntrypointContainer<Any?>>>)
	{
		if (!isGameTest) return
		// Ensure Archie GameTest handlers are registered even if Fabric's GameTest bootstrap runs
		// before the normal mod initializer entrypoint.
		Archie.init()

		val result: MutableList<EntrypointContainer<Any?>> = entrypointContainers.get().toMutableList()
		val mods = if (AEvents.MODS.isEmpty()) listOf(Archie.MOD) else AEvents.MODS
		for (mod in mods)
		{
			AEvents.REGISTER_GAME_TEST.invoker()(mod)
			for (clazz in testClasses.getOrPut(mod, ::mutableSetOf))
			{
				result.add(createEntrypointContainer(mod, clazz))
			}
		}
		entrypointContainers.set(result)
	}

	private fun createEntrypointContainer(
		mod: Mod,
		clazz: Class<*>,
	): EntrypointContainer<Any?> {
		return object : EntrypointContainer<Any?>
		{
			override fun getEntrypoint(): Any?
			{
				return clazz.kotlin.objectInstance ?: clazz.kotlin.primaryConstructor?.call()
			}

			override fun getProvider(): ModContainer?
			{
				return FabricLoader.getInstance().getModContainer(mod.modId).orElse(null)
			}
		}
	}
}