package net.kernelpanicsoft.archie.gametest.internal.tests

import androidx.compose.runtime.LaunchedEffect
import net.kernelpanicsoft.archie.gametest.ClientGameTest
import net.kernelpanicsoft.archie.gametest.ClientGameTestContext
import net.kernelpanicsoft.archie.gametest.LayerSelector
import net.kernelpanicsoft.archie.gametest.waitForScreen
import net.kernelpanicsoft.archie.gui.ComposeScreen
import net.kernelpanicsoft.archie.gui.layer.LocalLayerManager
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.layout.MeasurePolicy
import net.kernelpanicsoft.archie.gui.layout.MeasureResult
import net.kernelpanicsoft.archie.gui.modifiers.Constraints
import net.minecraft.network.chat.Component

private fun fixedSizeMeasurePolicy(width: Int, height: Int): MeasurePolicy = MeasurePolicy { _, _, _ ->
    MeasureResult(width, height) { }
}

/**
 * Client GameTest coverage for [ComposeScreen] rendering: that a [Layout] node is measured
 * with its policy's reported size, and that a layer pushed via `LocalLayerManager.modal` is
 * measured independently on top of the base layer.
 */
@Suppress("unused")
class ComposeRenderingTests {
    @ClientGameTest
    fun ClientGameTestContext.testComposeScreenMeasuresRenderableNode() {
        setScreen { RenderProbeScreen() }
        waitForScreen<RenderProbeScreen> {
            waitForLayer(0) {
                assertTrue(hasNode(RENDER_PROBE_NAME)) { "Expected render probe node to exist" }
                computeOnClient {
                    rootNode.measure(Constraints(maxWidth = 320, maxHeight = 240))
                }
                node(RENDER_PROBE_NAME) {
                    assertEquals(120, computeOnClient { node.width })
                    assertEquals(64, computeOnClient { node.height })
                }
            }
        }
    }

    @ClientGameTest
    fun ClientGameTestContext.testComposeScreenPushesModalLayer() {
        setScreen { ModalProbeScreen() }
        waitForScreen<ModalProbeScreen> {
            waitForLayer(1) {
                assertTrue(hasNode(MODAL_PROBE_NAME, LayerSelector.Top)) { "Expected modal probe node to exist" }
                computeOnClient {
                    rootNode.measure(Constraints(maxWidth = 320, maxHeight = 240))
                }
                node(MODAL_PROBE_NAME) {
                    assertEquals(96, computeOnClient { node.width })
                    assertEquals(48, computeOnClient { node.height })
                }
            }
        }
    }

    private class RenderProbeScreen : ComposeScreen(Component.literal("Compose Render Probe")) {
        override fun init() {
            super.init()
            start {
                Layout(
                    name = RENDER_PROBE_NAME,
                    measurePolicy = fixedSizeMeasurePolicy(120, 64),
                )
            }
        }
    }

    private class ModalProbeScreen : ComposeScreen(Component.literal("Compose Modal Probe")) {
        override fun init() {
            super.init()
            start {
                Layout(
                    name = BASE_PROBE_NAME,
                    measurePolicy = fixedSizeMeasurePolicy(160, 80),
                )
                val layerManager = LocalLayerManager.current
                LaunchedEffect(Unit) {
                    layerManager.modal(dismissOnClickOutside = false) {
                        Layout(
                            name = MODAL_PROBE_NAME,
                            measurePolicy = fixedSizeMeasurePolicy(96, 48),
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val BASE_PROBE_NAME = "ComposeBaseProbe"
        private const val MODAL_PROBE_NAME = "ComposeModalProbe"
        private const val RENDER_PROBE_NAME = "ComposeRenderProbe"
    }
}



