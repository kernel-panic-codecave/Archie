package com.withertech.archie.data.client.model

import dev.architectury.platform.Mod
import net.minecraft.data.PackOutput

abstract class ABlockModelProvider(output: PackOutput, mod: Mod, exitOnError: Boolean) :
	AModelProvider<ABlockModelBuilder>(output, mod, BLOCK_FOLDER, ::ABlockModelBuilder, exitOnError)
{
	override fun getName(): String = format("Block Models")
}