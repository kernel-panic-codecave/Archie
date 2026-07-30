package net.kernelpanicsoft.archie.test.gametest

import net.kernelpanicsoft.archie.events.AEvents
import net.kernelpanicsoft.archie.gametest.AGameTestEventObject
import net.kernelpanicsoft.archie.test.ArchieTest

internal fun AEvents.ArchieGameTestBuilder.archieTestGameTests()
{
	client {
		register<TestScreenGameTest>()
	}
}

internal object ArchieTestGameTest : AGameTestEventObject(ArchieTest.MOD)
{
	override fun AEvents.ArchieGameTestBuilder.handler() = archieTestGameTests()
}
