package net.kernelpanicsoft.archie.gametest.internal

import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.events.AEvents
import net.kernelpanicsoft.archie.gametest.AGameTestEventObject
import net.kernelpanicsoft.archie.gametest.internal.tests.CommonTests

internal object ArchieGameTest : AGameTestEventObject(Archie.MOD)
{
	override fun AEvents.ArchieGameTestBuilder.handler()
	{
		register<CommonTests>()
	}
}