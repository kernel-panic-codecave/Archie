package net.kernelpanicsoft.archie.gametest

import com.llamalad7.mixinextras.sugar.ref.LocalRef
import net.kernelpanicsoft.archie.events.AEvents
import dev.architectury.platform.Mod
import net.fabricmc.fabric.impl.gametest.FabricGameTestHelper
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import net.fabricmc.loader.api.entrypoint.EntrypointContainer
import kotlin.reflect.full.primaryConstructor

@Suppress("unused")
actual object AGameTestPlatform
{
	@Suppress("UnstableApiUsage")
	actual val isGameTest: Boolean
		get() = FabricGameTestHelper.ENABLED

	actual fun register(clazz: Class<*>, mod: Mod)
	{
		testClasses.getOrPut(mod, ::mutableListOf).add(clazz)
	}

	@JvmField
	internal val testClasses: MutableMap<Mod, MutableList<Class<*>>> = mutableMapOf()


}
