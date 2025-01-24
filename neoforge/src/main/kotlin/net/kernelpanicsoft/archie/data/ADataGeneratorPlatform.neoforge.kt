package net.kernelpanicsoft.archie.data

import net.kernelpanicsoft.archie.events.AEvents
import net.neoforged.fml.ModList
import net.neoforged.neoforge.data.event.GatherDataEvent

actual object ADataGeneratorPlatform
{
	actual val isDataGen: Boolean
		get() = System.getProperty("archie.datagen").toBoolean()
}