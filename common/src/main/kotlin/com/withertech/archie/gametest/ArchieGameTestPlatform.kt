package com.withertech.archie.gametest

import dev.architectury.injectables.annotations.ExpectPlatform
import dev.architectury.platform.Mod

object ArchieGameTestPlatform
{
	@get:ExpectPlatform
	@JvmStatic
	val isGameTest: Boolean
		get()
		{
			throw IllegalStateException()
		}


	@ExpectPlatform
	@JvmStatic
	fun register(clazz: Class<*>, mod: Mod)
	{
		throw IllegalStateException()
	}
}
