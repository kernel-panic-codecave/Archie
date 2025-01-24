package net.kernelpanicsoft.archie.config

import dev.architectury.platform.Mod
import me.shedaniel.clothconfig2.api.ConfigBuilder

internal object AConfigPlatformInternal
{
	@JvmField
	internal val screenHandlers: MutableMap<Mod, () -> ConfigBuilder> = mutableMapOf()
}