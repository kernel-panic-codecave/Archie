package net.kernelpanicsoft.archie.test.gametest

import net.kernelpanicsoft.archie.events.AGametestEvents
import net.kernelpanicsoft.archie.gametest.AGameTestEventObject
import net.kernelpanicsoft.archie.test.ArchieTest

internal fun AGametestEvents.ArchieGameTestBuilder.archieTestGameTests()
{
	client {
		register<TestScreenGameTest>()
		register<ComposeItemContainerMenuClientTests>()
	}
	server {
		register<ComposeItemContainerMenuTests>()
	}
	server {
		register<DataAttachmentTests>()
		register<CapabilityLookupTests>()
	}
}

internal object ArchieTestGameTest : AGameTestEventObject(ArchieTest.MOD)
{
	override fun AGametestEvents.ArchieGameTestBuilder.handler() = archieTestGameTests()
}
