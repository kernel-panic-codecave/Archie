package com.withertech.archie.config

import dev.architectury.platform.Mod
import me.shedaniel.clothconfig2.api.ConfigBuilder

expect object ArchieConfigPlatform
{
	fun registerScreenHandler(mod: Mod, builder: () -> ConfigBuilder)
}