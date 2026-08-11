package net.kernelpanicsoft.archie.data.client.model

import dev.architectury.platform.Mod
import net.minecraft.data.PackOutput

/** [AModelProvider] that generates block models under `models/block/`. */
abstract class ABlockModelProvider(output: PackOutput, mod: Mod, exitOnError: Boolean) :
	AModelProvider<ABlockModelBuilder>(output, mod, BLOCK_FOLDER, ::ABlockModelBuilder, exitOnError)
{
	override fun getName(): String = format("Block Models")
}