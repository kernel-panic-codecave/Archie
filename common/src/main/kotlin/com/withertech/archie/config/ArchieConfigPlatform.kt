package com.withertech.archie.config

import dev.architectury.injectables.annotations.ExpectPlatform
import dev.architectury.platform.Mod
import me.shedaniel.clothconfig2.api.ConfigBuilder

object ArchieConfigPlatform
{
	@ExpectPlatform
	@JvmStatic
	fun registerScreenHandler(mod: Mod, builder: () -> ConfigBuilder)
	{
		throw AssertionError()
	}
}