package com.withertech.archie.config

import dev.architectury.platform.Mod
import me.shedaniel.clothconfig2.api.ConfigBuilder

actual object ArchieConfigPlatform
{
	actual fun registerScreenHandler(mod: Mod, builder: () -> ConfigBuilder)
	{
		screenHandlers[mod] = builder
	}

	@JvmField
	internal val screenHandlers: MutableMap<Mod, () -> ConfigBuilder> = mutableMapOf()
}