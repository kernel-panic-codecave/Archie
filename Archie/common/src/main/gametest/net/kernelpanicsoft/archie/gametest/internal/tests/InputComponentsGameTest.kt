package net.kernelpanicsoft.archie.gametest.internal.tests

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import net.kernelpanicsoft.archie.gametest.ClientGameTest
import net.kernelpanicsoft.archie.gametest.ClientGameTestContext
import net.kernelpanicsoft.archie.gametest.waitForScreen
import net.kernelpanicsoft.archie.gui.ComposeScreen
import net.kernelpanicsoft.archie.gui.composables.basic.Text
import net.kernelpanicsoft.archie.gui.composables.input.Button
import net.kernelpanicsoft.archie.gui.composables.input.Checkbox
import net.kernelpanicsoft.archie.gui.composables.input.ColorPicker
import net.kernelpanicsoft.archie.gui.composables.input.RadioGroup
import net.kernelpanicsoft.archie.gui.composables.input.RadioOption
import net.kernelpanicsoft.archie.gui.composables.input.Slider
import net.kernelpanicsoft.archie.gui.composables.input.Switch
import net.kernelpanicsoft.archie.gui.composables.input.textfield.BasicTextField
import net.kernelpanicsoft.archie.gui.composables.theme.TextureStates
import net.kernelpanicsoft.archie.gui.layout.Arrangement
import net.kernelpanicsoft.archie.gui.layout.Column
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.sizeIn
import net.kernelpanicsoft.archie.gui.theme.Theme
import net.kernelpanicsoft.archie.gui.util.HsvColor
import net.kernelpanicsoft.archie.gui.util.KColor
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Client GameTest coverage for every standalone input composable in
 * `net.kernelpanicsoft.archie.gui.composables.input` - Checkbox, Switch, RadioGroup, Slider,
 * BasicTextField, ColorPicker, Button: hierarchy shape, click/hover/keypress/type input
 * handling, and (where the component is texture-state driven) which [TextureStates] key it
 * resolves for a given interaction - a texture-correctness check with no pixel comparison.
 *
 * Runs against [InputComponentsProbeScreen], a plain [ComposeScreen] with no world/menu/player,
 * since none of these composables need one - unlike slot/menu rendering, which does (see
 * `net.kernelpanicsoft.archie.test.gametest.TestScreenGameTest` in Archie-Test for that case).
 */
@Suppress("unused")
class InputComponentsGameTest {
    @ClientGameTest
    fun ClientGameTestContext.testHierarchyAndSizing() {
        setScreen { InputComponentsProbeScreen() }
        waitForScreen<InputComponentsProbeScreen> {
            waitForLayer(0) {
                node("Column") {
                    assertHasDescendant("Checkbox")
                    assertHasDescendant("Switch")
                    assertHasDescendant("RadioButton")
                    assertHasDescendant("Slider")
                    assertHasDescendant("TextFieldCore")
                    assertHasDescendant("ColorPicker")
                    assertHasDescendant("Button")

                    // Every RadioGroup option's Row wraps exactly one RadioButton + its label, in order.
                    node("Row") {
                        assertChildNames("RadioButton", "Text")
                    }

                    assertAllDescendantsSized()
                }
            }
        }
    }

    @ClientGameTest
    fun ClientGameTestContext.testCheckboxHoverAndClickRenderState() {
        setScreen { InputComponentsProbeScreen() }
        waitForScreen<InputComponentsProbeScreen> {
            waitForLayer(0) {
                node("Checkbox") {
                    assertRenderState(TextureStates.DEFAULT)

                    hover()
                    waitForComposeIdle()
                    assertRenderState(TextureStates.HOVERED)

                    click()
                    waitForComposeIdle()
                    assertRenderState(TextureStates.CLICKED_AND_HOVERED) { "Expected checkbox to be both checked and hovered right after a click at its own center" }

                    click()
                    waitForComposeIdle()
                    assertRenderState(TextureStates.HOVERED) { "Expected checkbox to be unchecked again after a second click" }
                }
            }
        }
    }

    @ClientGameTest
    fun ClientGameTestContext.testSwitchHoverAndClickRenderState() {
        setScreen { InputComponentsProbeScreen() }
        waitForScreen<InputComponentsProbeScreen> {
            waitForLayer(0) {
                node("Switch") {
                    // Probe's initial `switched = true`.
                    assertRenderState(TextureStates.CLICKED)

                    click()
                    waitForComposeIdle()
                    assertRenderState(TextureStates.DEFAULT) { "Expected switch to be off after toggling its initial on state" }

                    hover()
                    waitForComposeIdle()
                    assertRenderState(TextureStates.HOVERED)
                }
            }
        }
    }

    @ClientGameTest
    fun ClientGameTestContext.testRadioGroupSelectsOptionExclusively() {
        val selected = AtomicReference("alpha")
        setScreen { InputComponentsProbeScreen(onRadioSelected = { selected.set(it) }) }
        waitForScreen<InputComponentsProbeScreen> {
            waitForLayer(0) {
                node("Column") {
                    val options = nodes("RadioButton")
                    assertTrue(options.size == 3) { "Expected 3 RadioButton options, found ${options.size}" }

                    // Probe's initial `radio = "alpha"` (declaration order: alpha, beta, gamma).
                    options[0] {
                        assertRenderState(TextureStates.CLICKED) { "Expected the first (initially selected) radio option to render CLICKED" }
                    }
                    options[1] { assertRenderState(TextureStates.DEFAULT) }
                    options[2] { assertRenderState(TextureStates.DEFAULT) }

                    // Select "beta" (options[1]) and verify selection moved there exclusively.
                    options[1] { click() }
                    waitForComposeIdle()

                    assertEquals("beta", selected.get()) { "Expected clicking the second radio option to select 'beta'" }
                    options[0] {
                        assertRenderState(TextureStates.DEFAULT) { "Expected the first option to deselect once a different option is chosen" }
                    }
                    options[1] { assertRenderState(TextureStates.CLICKED) }
                    options[2] { assertRenderState(TextureStates.DEFAULT) }

                    // Clicking an already-selected option is a documented no-op (see RadioButtonCore).
                    options[1] { click() }
                    waitForComposeIdle()
                    assertEquals("beta", selected.get()) { "Expected re-clicking the selected option to stay a no-op" }
                }
            }
        }
    }

    @ClientGameTest
    fun ClientGameTestContext.testSliderHoverAndDragRenderState() {
        setScreen { InputComponentsProbeScreen() }
        waitForScreen<InputComponentsProbeScreen> {
            waitForLayer(0) {
                node("Slider") {
                    assertRenderState(TextureStates.DEFAULT)

                    hover()
                    waitForComposeIdle()
                    assertRenderState(TextureStates.HOVERED)

                    context.getInput().holdMouse(0)
                    waitForComposeIdle()
                    assertRenderState(TextureStates.CLICKED) { "Expected slider to report the dragging (CLICKED) state while the mouse button is held" }

                    context.getInput().releaseMouse(0)
                    waitForComposeIdle()
                    assertRenderState(TextureStates.HOVERED) { "Expected slider to return to hovered after releasing the drag" }
                }
            }
        }
    }

    @ClientGameTest
    fun ClientGameTestContext.testButtonClickFiresCallbackAndRenderState() {
        val clicked = AtomicBoolean(false)
        setScreen { InputComponentsProbeScreen(onButtonClick = { clicked.set(true) }) }
        waitForScreen<InputComponentsProbeScreen> {
            waitForLayer(0) {
                node("Button") {
                    assertRenderState(TextureStates.DEFAULT)

                    hover()
                    waitForComposeIdle()
                    assertRenderState(TextureStates.HOVERED)

                    click()
                    waitForComposeIdle()

                    assertTrue(clicked.get()) { "Expected Button's onClick callback to have fired" }
                }
            }
        }
    }

    @ClientGameTest
    fun ClientGameTestContext.testTextFieldTypeAndBackspace() {
        val typed = AtomicReference("")
        setScreen { InputComponentsProbeScreen(onTextChanged = { typed.set(it) }) }
        waitForScreen<InputComponentsProbeScreen> {
            waitForLayer(0) {
                node("TextFieldCore") {
                    click()
                    waitForComposeIdle()

                    type("hello")
                    waitForComposeIdle()
                    assertEquals("hello", typed.get()) { "Expected typed characters to reach onValueChange" }

                    pressKey(GLFW.GLFW_KEY_BACKSPACE)
                    waitForComposeIdle()
                    assertEquals("hell", typed.get()) { "Expected backspace to remove the last typed character" }

                    assertAllDescendantsSized()
                }
            }
        }
    }

    @ClientGameTest
    fun ClientGameTestContext.testColorPickerInteractionUpdatesColor() {
        val initial = HsvColor.from(KColor.CYAN)
        val changed = AtomicReference<HsvColor?>(null)
        setScreen { InputComponentsProbeScreen(initialColor = initial, onColorChanged = { changed.set(it) }) }
        waitForScreen<InputComponentsProbeScreen> {
            waitForLayer(0) {
                node("ColorPicker") {
                    assertChildNames("SaturationValueArea", "AlphaBar", "HueBar")
                    assertAllDescendantsSized()

                    node("HueBar") { click() }
                    waitForComposeIdle()

                    assertTrue(changed.get() != null) { "Expected clicking the hue bar to report a color change" }
                }
            }
        }
    }
}

private class InputComponentsProbeScreen(
    private val initialColor: HsvColor = HsvColor.from(KColor.CYAN),
    private val onButtonClick: () -> Unit = {},
    private val onTextChanged: (String) -> Unit = {},
    private val onColorChanged: (HsvColor) -> Unit = {},
    private val onRadioSelected: (String) -> Unit = {},
) : ComposeScreen(Component.literal("Input Components Probe")) {
    override fun init() {
        super.init()
        start {
            Theme {
                var checked by remember { mutableStateOf(false) }
                var switched by remember { mutableStateOf(true) }
                var radio by remember { mutableStateOf("alpha") }
                var slider by remember { mutableStateOf(0.35f) }
                var text by remember { mutableStateOf("") }
                var color by remember { mutableStateOf(initialColor) }

                Column(verticalArrangement = Arrangement.spacedBy(4)) {
                    Checkbox(checked = checked, onCheckedChange = { checked = it })
                    Switch(checked = switched, onCheckedChange = { switched = it })
                    RadioGroup(
                        options = listOf(
                            RadioOption("alpha", Component.literal("Alpha")),
                            RadioOption("beta", Component.literal("Beta")),
                            RadioOption("gamma", Component.literal("Gamma")),
                        ),
                        selected = radio,
                        onSelected = { radio = it; onRadioSelected(it) },
                    )
                    Slider(value = slider, onValueChange = { slider = it }, steps = 10)
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it; onTextChanged(it) },
                        modifier = Modifier.sizeIn(minWidth = 120, maxWidth = 180),
                    )
                    ColorPicker(
                        color = color,
                        onColorChanged = { color = it; onColorChanged(it) },
                        modifier = Modifier.sizeIn(minWidth = 150, minHeight = 90, maxWidth = 170, maxHeight = 100),
                    )
                    Button(onClick = { onButtonClick() }) {
                        Text(Component.literal("Click me"), dropShadow = false)
                    }
                }
            }
        }
    }
}
