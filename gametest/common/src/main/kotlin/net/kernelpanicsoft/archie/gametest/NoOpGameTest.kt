package net.kernelpanicsoft.archie.gametest

import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper

/**
 * A trivially-succeeding placeholder, registered by [AGameTestPlatformInternal] on each loader
 * when a mod's [net.kernelpanicsoft.archie.gametest.platform.AGameTestModFilter]-selected suite has no real test functions for the current
 * [net.kernelpanicsoft.archie.gametest.platform.AGameTestSide] - e.g. a mod with only client-side coverage (like Archie-Test, whose own suite
 * registers just [net.kernelpanicsoft.archie.test.gametest.TestScreenGameTest]) running its
 * server invocation. Vanilla's `GameTestServer` refuses to boot with zero registered test
 * functions at all (`IllegalArgumentException: No test functions were given!`); this keeps that
 * boot trivially satisfied instead of crashing the whole invocation.
 */
@Suppress("unused")
class NoOpGameTest {
	@GameTest(template = "archie:gametest/empty")
	fun GameTestHelper.testNoOpPlaceholder() {
		succeed()
	}
}
