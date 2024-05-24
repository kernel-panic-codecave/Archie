package com.withertech.archie.config.fabric

import dev.architectury.platform.Mod
import me.shedaniel.clothconfig2.api.ConfigBuilder

object ArchieConfigPlatformImpl
{
	@JvmStatic
	fun registerScreenHandler(mod: Mod, builder: () -> ConfigBuilder)
	{
		screenHandlers[mod] = builder
	}

	@JvmField
	internal val screenHandlers: MutableMap<Mod, () -> ConfigBuilder> = mutableMapOf()
}