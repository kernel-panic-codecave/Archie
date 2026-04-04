package net.kernelpanicsoft.archie.gametest.internal

import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.events.AEvents
import net.kernelpanicsoft.archie.gametest.AGameTestEventObject
import net.kernelpanicsoft.archie.gametest.internal.tests.ArchieItemHandlerTests
import net.kernelpanicsoft.archie.gametest.internal.tests.BlockEntityNBTHolderTests
import net.kernelpanicsoft.archie.gametest.internal.tests.BlockEntityStateManagerTests
import net.kernelpanicsoft.archie.gametest.internal.tests.ComposeRenderingTests

const val EMPTY = "archie:gametest/empty"

internal fun AEvents.ArchieGameTestBuilder.archieGameTests()
{
	common {
		
	}
	client {
		register<ComposeRenderingTests>()
	}
	server {
		register<BlockEntityStateManagerTests>()
		register<BlockEntityNBTHolderTests>()
		register<ArchieItemHandlerTests>()
	}
}

internal object ArchieGameTest : AGameTestEventObject(Archie.MOD)
{
	override fun AEvents.ArchieGameTestBuilder.handler() = archieGameTests()
}