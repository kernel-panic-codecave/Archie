package net.kernelpanicsoft.archie.gametest.internal.tests

import androidx.compose.runtime.Composable
import net.kernelpanicsoft.archie.gametest.ClientGameTest
import net.kernelpanicsoft.archie.gametest.ClientGameTestContext
import net.kernelpanicsoft.archie.gametest.TestNodeScope
import net.kernelpanicsoft.archie.gametest.waitForScreen
import net.kernelpanicsoft.archie.gui.ComposeScreen
import net.kernelpanicsoft.archie.gui.composables.basic.HorizontalDivider
import net.kernelpanicsoft.archie.gui.composables.basic.Icon
import net.kernelpanicsoft.archie.gui.composables.basic.Text
import net.kernelpanicsoft.archie.gui.composables.containers.Collapsible
import net.kernelpanicsoft.archie.gui.composables.containers.Panel
import net.kernelpanicsoft.archie.gui.composables.containers.Scrollable
import net.kernelpanicsoft.archie.gui.composables.containers.ScrollableState
import net.kernelpanicsoft.archie.gui.composables.containers.TabPanel
import net.kernelpanicsoft.archie.gui.composables.theme.TextureStates
import net.kernelpanicsoft.archie.gui.layout.Arrangement
import net.kernelpanicsoft.archie.gui.layout.Column
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.layout.MeasureResult
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.height
import net.kernelpanicsoft.archie.gui.modifiers.width
import net.kernelpanicsoft.archie.gui.theme.Theme
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Client GameTest coverage for the remaining (non-input) composables: [Panel]/[net.kernelpanicsoft.archie.gui.composables.containers.Surface],
 * [HorizontalDivider], [Icon], [Text], [Collapsible], [Scrollable], and [TabPanel].
 *
 * Each gets its own small, single-purpose probe screen rather than one shared screen, since
 * several of these composables reuse generic container node names ("Row", "Column") - isolating
 * them keeps [TestNodeScope.node] lookups unambiguous without needing a unique-name redesign.
 */
@Suppress("unused")
class LayoutComponentsGameTest {
    @ClientGameTest
    fun ClientGameTestContext.testPanelDividerIconTextHierarchyAndSizing() {
        setScreen { PanelDisplayProbeScreen() }
        waitForScreen<PanelDisplayProbeScreen> {
            waitForLayer(0) {
                node("Surface") {
                    // Panel wraps its content in a padded Box inside the themed Surface.
                    node("Box") {
                        assertChildNames("Text", "Spacer", "Texture")
                    }
                    assertAllDescendantsSized()
                }
            }
        }
    }

    @ClientGameTest
    fun ClientGameTestContext.testCollapsibleTogglesContentOnHeaderClick() {
        val toggled = AtomicBoolean(false)
        setScreen { CollapsibleProbeScreen(initiallyExpanded = false, onToggled = { toggled.set(true) }) }
        waitForScreen<CollapsibleProbeScreen> {
            waitForLayer(0) {
                node("Column") {
                    assertTrue(!hasDescendant("CollapsibleContent")) { "Expected content hidden while collapsed" }

                    node("Row") { click() }
                    waitForComposeIdle()

                    assertTrue(toggled.get()) { "Expected onToggled to fire on header click" }
                    assertHasDescendant("CollapsibleContent")
                    assertAllDescendantsSized()

                    node("Row") { click() }
                    waitForComposeIdle()
                    assertTrue(!hasDescendant("CollapsibleContent")) { "Expected content hidden again after collapsing" }
                }
            }
        }
    }

    @ClientGameTest
    fun ClientGameTestContext.testScrollableScrollsContent() {
        val scrollState = ScrollableState()
        setScreen { ScrollableProbeScreen(scrollState) }
        waitForScreen<ScrollableProbeScreen> {
            waitForLayer(0) {
                node("Scrollable") {
                    assertAllDescendantsSized()
                    assertEquals(0.0, computeOnClient { scrollState.scrollOffset })

                    scroll(y = -10.0)
                    waitForComposeIdle()

                    val offsetAfterScroll = computeOnClient { scrollState.scrollOffset }
                    assertTrue(offsetAfterScroll > 0.0) { "Expected scrolling over the Scrollable node to move scrollOffset, got $offsetAfterScroll" }
                }
            }
        }
    }

    @ClientGameTest
    fun ClientGameTestContext.testScrollDoesNotCorruptContentX() {
        // Regression test for IntCoordinates' packed-Long constructor: a negative y sign-extends
        // across the bits x is packed into, so any negatively-offset content (exactly what
        // Scrollable produces once scrolled - placeAt(0, -scrollPos)) previously decoded with
        // x forced to -1 no matter its real value.
        val scrollState = ScrollableState()
        setScreen { ScrollableProbeScreen(scrollState) }
        waitForScreen<ScrollableProbeScreen> {
            waitForLayer(0) {
                node("Scrollable") {
                    val xBefore = computeOnClient { node.children.first().x }
                    assertEquals(0, xBefore)

                    scroll(y = -10.0)
                    waitForComposeIdle()

                    val (scrollOffset, xAfter) = computeOnClient { scrollState.scrollOffset to node.children.first().x }
                    assertTrue(scrollOffset > 0.0) { "Expected scrolling over the Scrollable node to move scrollOffset, got $scrollOffset" }
                    assertEquals(0, xAfter) { "Expected the scrolled content's x to stay 0 (only y should move), got $xAfter" }
                }
            }
        }
    }

    @ClientGameTest
    fun ClientGameTestContext.testTabPanelSwitchesActiveTabOnClick() {
        setScreen { TabPanelProbeScreen() }
        waitForScreen<TabPanelProbeScreen> {
            waitForLayer(0) {
                node("Column") {
                    val tabs = nodes("Tab")
                    assertTrue(tabs.size == 2) { "Expected 2 Tab headers, found ${tabs.size}" }

                    assertHasDescendant("FirstTabMarker")
                    assertTrue(!hasDescendant("SecondTabMarker")) { "Expected only the first tab's content to be composed initially" }
                    tabs[0] {
                        assertRenderState(TextureStates.CLICKED) {
                            "Expected the first (initially active) tab to render CLICKED"
                        }
                    }
                    tabs[1] { click() }
                    waitForComposeIdle()

                    assertHasDescendant("SecondTabMarker")
                    assertTrue(!hasDescendant("FirstTabMarker")) { "Expected only the second tab's content to be composed after switching" }
                    tabs[1] { assertRenderState(TextureStates.CLICKED) }
                    tabs[0] { assertRenderState(TextureStates.DEFAULT) }
                }
            }
        }
    }
}

private class PanelDisplayProbeScreen : ComposeScreen(Component.literal("Panel Display Probe")) {
    override fun init() {
        super.init()
        start {
            Theme {
                Panel {
                    Text(Component.literal("Panel content"), dropShadow = false)
                    HorizontalDivider()
                    Icon(texture = ResourceLocation.withDefaultNamespace("textures/item/porkchop.png"), size = 16)
                }
            }
        }
    }
}

private class CollapsibleProbeScreen(
    private val initiallyExpanded: Boolean,
    private val onToggled: (Boolean) -> Unit,
) : ComposeScreen(Component.literal("Collapsible Probe")) {
    override fun init() {
        super.init()
        start {
            Theme {
                Column {
                    Collapsible(
                        title = Component.literal("Section"),
                        initiallyExpanded = initiallyExpanded,
                        onToggled = onToggled,
                    ) {
                        Text(Component.literal("Collapsible body"), dropShadow = false)
                    }
                }
            }
        }
    }
}

private class ScrollableProbeScreen(
    private val scrollState: ScrollableState,
) : ComposeScreen(Component.literal("Scrollable Probe")) {
    override fun init() {
        super.init()
        start {
            Theme {
                Scrollable(state = scrollState, modifier = Modifier.height(80).width(120)) {
                    Column(verticalArrangement = Arrangement.spacedBy(2)) {
                        repeat(60) { index ->
                            Text(Component.literal("Row ${index + 1}"), dropShadow = false)
                        }
                    }
                }
            }
        }
    }
}

/** A zero-size, invisible node whose mere presence in the tree marks which branch was composed. */
@Composable
private fun Marker(name: String) {
    Layout(name = name, measurePolicy = { _, _, _ -> MeasureResult(0, 0) {} })
}

private class TabPanelProbeScreen : ComposeScreen(Component.literal("Tab Panel Probe")) {
    override fun init() {
        super.init()
        start {
            Theme {
                Column {
                    TabPanel {
                        tab("first", Component.literal("First")) {
                            Marker("FirstTabMarker")
                            Text(Component.literal("First tab content"), dropShadow = false)
                        }
                        tab("second", Component.literal("Second")) {
                            Marker("SecondTabMarker")
                            Text(Component.literal("Second tab content"), dropShadow = false)
                        }
                    }
                }
            }
        }
    }
}
