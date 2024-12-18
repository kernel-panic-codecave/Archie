package com.withertech.archie.gametest

import dev.architectury.platform.Mod

expect object AGameTestPlatform
{
	val isGameTest: Boolean

	fun register(clazz: Class<*>, mod: Mod)
}
