package net.kernelpanicsoft.archie.gametest.internal

import net.kernelpanicsoft.archie.Archie
import net.kernelpanicsoft.archie.events.gametest.AGametestEvents
import net.kernelpanicsoft.archie.gametest.AGameTestEventObject
import net.kernelpanicsoft.archie.gametest.internal.tests.ArchieItemHandlerTests
import net.kernelpanicsoft.archie.gametest.internal.tests.BlockEntityNBTHolderTests
import net.kernelpanicsoft.archie.gametest.internal.tests.BlockEntityStateManagerTests
import net.kernelpanicsoft.archie.gametest.internal.tests.ComposeRenderingTests
import net.kernelpanicsoft.archie.gametest.internal.tests.InputComponentsGameTest
import net.kernelpanicsoft.archie.gametest.internal.tests.LayoutComponentsGameTest
import net.kernelpanicsoft.archie.gametest.internal.tests.ModalComponentsGameTest
import net.kernelpanicsoft.archie.gametest.internal.tests.TreeSerializationGameTest

/**
 * ID of the empty structure template used by every GameTest in this suite; GameTests that don't
 * need a specific structure should reference this via `@GameTest(template = EMPTY)`. Explicitly
 * namespaced - Fabric has no custom template-namespace resolution of its own and relies entirely
 * on this string being a complete id. NeoForge's `GameTestHooksMixin` mixes `turnMethodIntoTestFunction`
 * itself (not just its two namespace/prefix helper methods) to use this value verbatim instead of
 * NeoForge's own unconditional `getTemplateNamespace(method) + ":"` wrap, which would otherwise
 * double the "archie:" prefix already present here.
 */
const val EMPTY = "archie:gametest/empty"

/**
 * Registers Archie's own internal GameTest suite (the tests under [net.kernelpanicsoft.archie.gametest.internal.tests])
 * against the given builder, scoped by the environment each suite needs to run in.
 */
internal fun AGametestEvents.ArchieGameTestBuilder.archieGameTests()
{
	common {

	}
	client {
		register<ComposeRenderingTests>()
		register<InputComponentsGameTest>()
		register<LayoutComponentsGameTest>()
		register<ModalComponentsGameTest>()
		register<TreeSerializationGameTest>()
	}
	server {
		register<BlockEntityStateManagerTests>()
		register<BlockEntityNBTHolderTests>()
		register<ArchieItemHandlerTests>()
	}
}

/**
 * Registration entry point for Archie's internal GameTest suite, hooked into [AGametestEvents]'s
 * GameTest registration handler for [Archie.MOD].
 */
internal object ArchieGameTest : AGameTestEventObject(Archie.MOD)
{
	override fun AGametestEvents.ArchieGameTestBuilder.handler() = archieGameTests()
}