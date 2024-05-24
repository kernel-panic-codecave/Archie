package com.withertech.archie.data.client.model

import dev.architectury.platform.Mod
import net.minecraft.data.PackOutput

abstract class ArchieBlockModelProvider(output: PackOutput, mod: Mod, exitOnError: Boolean) :
	ArchieModelProvider<BlockModelBuilder>(output, mod, BLOCK_FOLDER, ::BlockModelBuilder, exitOnError)
{
	override fun getName(): String = format("Block Models")
}