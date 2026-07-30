package net.kernelpanicsoft.archie.gametest.internal

import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.events.AEvents
import net.kernelpanicsoft.archie.gametest.AGameTestEventObject
import net.kernelpanicsoft.archie.gametest.internal.tests.ArchieItemHandlerTests
import net.kernelpanicsoft.archie.gametest.internal.tests.BlockEntityNBTHolderTests
import net.kernelpanicsoft.archie.gametest.internal.tests.BlockEntityStateManagerTests
import net.kernelpanicsoft.archie.gametest.internal.tests.ComposeRenderingTests

/**
 * ID of the empty structure template used by every GameTest in this suite; GameTests that don't
 * need a specific structure should reference this via `@GameTest(template = EMPTY)`.
 */
const val EMPTY = "archie:gametest/empty"

/**
 * Registers Archie's own internal GameTest suite (the tests under [net.kernelpanicsoft.archie.gametest.internal.tests])
 * against the given builder, scoped by the environment each suite needs to run in.
 */
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

/**
 * Registration entry point for Archie's internal GameTest suite, hooked into [AEvents]'s
 * GameTest registration handler for [Archie.MOD].
 */
internal object ArchieGameTest : AGameTestEventObject(Archie.MOD)
{
	override fun AEvents.ArchieGameTestBuilder.handler() = archieGameTests()
}