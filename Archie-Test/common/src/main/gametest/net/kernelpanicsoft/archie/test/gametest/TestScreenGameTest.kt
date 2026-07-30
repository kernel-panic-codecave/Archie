package net.kernelpanicsoft.archie.test.gametest

import net.kernelpanicsoft.archie.gametest.*
import net.kernelpanicsoft.archie.test.BlockRegistry
import net.kernelpanicsoft.archie.test.TestScreen
import net.kernelpanicsoft.archie.test.TestTile

/**
 * End-to-end UI smoke test for [TestScreen], the sample mod's Compose widget showcase.
 *
 * Exercises the framework the way a consuming mod actually uses it: opening a real
 * [net.kernelpanicsoft.archie.gui.ComposeContainerScreen] backed by a block entity menu,
 * then driving one of its `LayerStackManager.confirmDialog` interactions end to end.
 */
@Suppress("unused")
class TestScreenGameTest {
    @ClientGameTest
    fun ClientGameTestContext.testShowcaseScreenOpensAndConfirmDialogRoundTrips() {
        withWorld {
            withSingleplayer {
                val player = waitForPlayer()
                val pos = player.blockPosition().above()
                placeTileAndWaitForScreen<TestTile, TestScreen>(pos, BlockRegistry.TestBlock.defaultBlockState()) {
                    assertEquals(1, layerCount) {
                        "Expected only the base layer right after opening the showcase screen"
                    }

                    node("Button", layer = LayerSelector.Base) { click() }

                    waitFor { _ -> layerCount == 2 }
                    assertEquals(2, layerCount) {
                        "Expected a modal ConfirmDialog layer after clicking the sync button"
                    }

                    node("Button", layer = LayerSelector.Top) { click() }

                    waitFor { _ -> layerCount == 1 }
                    assertEquals(1, layerCount) {
                        "Expected the ConfirmDialog to close after confirming"
                    }
                }
            }
        }
    }
}
