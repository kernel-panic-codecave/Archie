package com.withertech.archie.gametest

import dev.architectury.platform.Mod

expect object ArchieGameTestPlatform
{
	val isGameTest: Boolean

	fun register(clazz: Class<*>, mod: Mod)
}
