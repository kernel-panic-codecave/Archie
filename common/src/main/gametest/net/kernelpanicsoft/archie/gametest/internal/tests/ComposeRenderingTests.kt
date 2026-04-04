package net.kernelpanicsoft.archie.gametest.internal.tests

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import net.kernelpanicsoft.archie.gametest.AClientGameTest
import net.kernelpanicsoft.archie.gametest.AClientGameTestContext
import net.kernelpanicsoft.archie.gui.ComposeScreen
import net.kernelpanicsoft.archie.gui.layer.LayerStackManager
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.layout.MeasurePolicy
import net.kernelpanicsoft.archie.gui.layout.MeasureResult
import net.kernelpanicsoft.archie.gui.modifiers.Constraints
import net.kernelpanicsoft.archie.util.getReflection
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

@Suppress("unused")
class ComposeRenderingTests {
    @AClientGameTest
    fun testComposeScreenMeasuresRenderableNode(context: AClientGameTestContext) {
        context.ensureClientWorld()

        val screen = RenderProbeScreen()
        context.setScreen { screen }
        context.waitForScreen(RenderProbeScreen::class.java)

        val layerManager = context.computeOnClient { screen.layerManager() }
        context.assertEquals(1, layerManager.layers.size)

        val rootNode = context.computeOnClient { layerManager.layers.first().rootNode }
        val probeNode = context.computeOnClient { rootNode.findNode(RENDER_PROBE_NAME) }
        context.assertTrue(probeNode != null) { "Expected render probe node to exist" }

        context.computeOnClient {
            rootNode.measure(Constraints(maxWidth = 320, maxHeight = 240))
        }

        context.assertEquals(120, context.computeOnClient { probeNode!!.width })
        context.assertEquals(64, context.computeOnClient { probeNode!!.height })
    }

    @AClientGameTest
    fun testComposeScreenPushesModalLayer(context: AClientGameTestContext) {
        context.ensureClientWorld()

        val screen = ModalProbeScreen()
        context.setScreen { screen }
        context.waitForScreen(ModalProbeScreen::class.java)
        context.waitFor { _ -> context.computeOnClient { screen.layerManager().layers.size } == 2 }

        val layerManager = context.computeOnClient { screen.layerManager() }
        context.assertEquals(2, layerManager.layers.size)

        val modalLayer = context.computeOnClient { layerManager.layers.last() }
        val modalNode = context.computeOnClient { modalLayer.findNode(MODAL_PROBE_NAME) }
        context.assertTrue(modalNode != null) { "Expected modal probe node to exist" }

        context.computeOnClient {
            modalLayer.rootNode.measure(Constraints(maxWidth = 320, maxHeight = 240))
        }

        context.assertEquals(96, context.computeOnClient { modalNode!!.width })
        context.assertEquals(48, context.computeOnClient { modalNode!!.height })
    }

    private fun AClientGameTestContext.ensureClientWorld() {
        if (computeOnClient { Minecraft.getInstance().level != null }) return
        worldBuilder().create()
    }

    private inline fun <reified S : ComposeScreen> S.layerManager(): LayerStackManager = getReflection("layerManager")

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
                val layerManager = net.kernelpanicsoft.archie.gui.layer.LocalLayerManager.current
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

    private fun fixedSizeMeasurePolicy(width: Int, height: Int): MeasurePolicy = MeasurePolicy { _, _, _ ->
        MeasureResult(width, height) { }
    }

    companion object {
        private const val BASE_PROBE_NAME = "ComposeBaseProbe"
        private const val MODAL_PROBE_NAME = "ComposeModalProbe"
        private const val RENDER_PROBE_NAME = "ComposeRenderProbe"
    }
}

