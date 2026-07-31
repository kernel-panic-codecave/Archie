package net.kernelpanicsoft.archie.gametest.internal.tests

import net.kernelpanicsoft.archie.gametest.ClientGameTest
import net.kernelpanicsoft.archie.gametest.ClientGameTestContext
import net.kernelpanicsoft.archie.gametest.LayerSelector
import net.kernelpanicsoft.archie.gametest.waitForScreen
import net.kernelpanicsoft.archie.gui.ComposeScreen
import net.kernelpanicsoft.archie.gui.composables.basic.Text
import net.kernelpanicsoft.archie.gui.composables.input.Button
import net.kernelpanicsoft.archie.gui.composables.modal.ModalChoice
import net.kernelpanicsoft.archie.gui.composables.theme.TextureStates
import net.kernelpanicsoft.archie.gui.layer.LocalLayerManager
import net.kernelpanicsoft.archie.gui.layout.Column
import net.minecraft.network.chat.Component
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Client GameTest coverage for the built-in modal dialogs (`net.kernelpanicsoft.archie.gui.composables.modal`):
 * [net.kernelpanicsoft.archie.gui.composables.modal.AlertDialog],
 * [net.kernelpanicsoft.archie.gui.composables.modal.PromptDialog],
 * [net.kernelpanicsoft.archie.gui.composables.modal.ChoiceDialog], and
 * [net.kernelpanicsoft.archie.gui.composables.modal.ConfirmDialog] - modal-layer hierarchy,
 * typing into a prompt, disabled-button texture state, and confirm/cancel/dismiss flows.
 *
 * Runs against [ModalComponentsProbeScreen], a plain [ComposeScreen] (a [net.kernelpanicsoft.archie.gui.LayerManagerProvider]
 * on its own, same as [net.kernelpanicsoft.archie.gui.ComposeContainerScreen]), so no world,
 * menu, or player is needed. `net.kernelpanicsoft.archie.test.gametest.TestScreenGameTest` in
 * Archie-Test already covers a [net.kernelpanicsoft.archie.gui.composables.modal.ConfirmDialog]
 * end to end against the real block-entity-backed screen; this file exercises it (plus the other
 * three dialog kinds) at the lightweight-probe level instead of duplicating that coverage.
 */
@Suppress("unused")
class ModalComponentsGameTest {
    @ClientGameTest
    fun ClientGameTestContext.testAlertDialogConfirmDismisses() {
        val confirmed = AtomicBoolean(false)
        setScreen { ModalComponentsProbeScreen(onAlertConfirm = { confirmed.set(true) }) }
        waitForScreen<ModalComponentsProbeScreen> {
            assertEquals(1, layerCount)
            val triggers = baseLayer.rootNode { nodes("Button") }
            triggers[0] { click() } // "Open Alert"
            waitFor { _ -> layerCount == 2 }

            node("Surface", layer = LayerSelector.Top) {
                val buttons = nodes("Button")
                assertTrue(buttons.size == 1) { "Expected AlertDialog to have exactly 1 action button, found ${buttons.size}" }
                buttons[0] { click() }
            }

            waitFor { _ -> layerCount == 1 }
            assertTrue(confirmed.get()) { "Expected AlertDialog's onConfirm to fire" }
        }
    }

    @ClientGameTest
    fun ClientGameTestContext.testPromptDialogValidatorDisablesConfirmUntilTyped() {
        val confirmedValue = AtomicReference<String?>(null)
        setScreen { ModalComponentsProbeScreen(onPromptConfirm = { confirmedValue.set(it) }) }
        waitForScreen<ModalComponentsProbeScreen> {
            val triggers = baseLayer.rootNode { nodes("Button") }
            triggers[1] { click() } // "Open Prompt"
            waitFor { _ -> layerCount == 2 }

            node("Surface", layer = LayerSelector.Top) {
                assertHasDescendant("TextFieldCore")
                val buttons = nodes("Button")
                assertTrue(buttons.size == 2) { "Expected PromptDialog to have 2 action buttons (cancel, confirm), found ${buttons.size}" }
                val confirmButton = buttons[1]

                // Empty initial value fails the `it.isNotBlank()` validator - confirm starts disabled.
                confirmButton {
                    assertRenderState(TextureStates.DISABLED) { "Expected the confirm button to render DISABLED while the field is blank" }
                }

                node("TextFieldCore") { click(); type("hello") }
                waitForComposeIdle()

                confirmButton {
                    assertTrue(renderState != TextureStates.DISABLED) { "Expected the confirm button to no longer be disabled once the field has text" }
                }

                confirmButton { click() }
            }

            waitFor { _ -> layerCount == 1 }
            assertEquals("hello", confirmedValue.get()) { "Expected PromptDialog's onConfirm to report the typed value" }
        }
    }

    @ClientGameTest
    fun ClientGameTestContext.testChoiceDialogSelectsEnabledOptionAndSkipsDisabled() {
        val selected = AtomicReference<String?>(null)
        setScreen { ModalComponentsProbeScreen(onChoiceSelected = { selected.set(it) }) }
        waitForScreen<ModalComponentsProbeScreen> {
            val triggers = baseLayer.rootNode { nodes("Button") }
            triggers[2] { click() } // "Open Choice"
            waitFor { _ -> layerCount == 2 }
            waitForComposeIdle()

            node("Surface", layer = LayerSelector.Top) {
                val buttons = nodes("Button")
                // 3 choices ("alpha", "beta", disabled "locked") + 1 cancel button, in that order.
                assertTrue(buttons.size == 4) { "Expected ChoiceDialog to have 4 buttons (3 choices + cancel), found ${buttons.size}" }

                buttons[2] {
                    assertRenderState(TextureStates.DISABLED) { "Expected the disabled 'locked' choice to render DISABLED" }
                }

                buttons[1] { click() } // "beta"
            }

            waitFor { _ -> layerCount == 1 }
            assertEquals("beta", selected.get()) { "Expected ChoiceDialog's onSelected to report the clicked choice" }
        }
    }

    @ClientGameTest
    fun ClientGameTestContext.testConfirmDialogConfirmAndCancelFlows() {
        val confirmed = AtomicBoolean(false)
        val cancelled = AtomicBoolean(false)
        setScreen {
            ModalComponentsProbeScreen(
                onConfirmDialogConfirm = { confirmed.set(true) },
                onConfirmDialogCancel = { cancelled.set(true) },
            )
        }
        waitForScreen<ModalComponentsProbeScreen> {
            val triggers = baseLayer.rootNode { nodes("Button") }

            // Confirm path.
            triggers[3] { click() } // "Open Confirm"
            waitFor { _ -> layerCount == 2 }
            node("Surface", layer = LayerSelector.Top) {
                val buttons = nodes("Button")
                assertTrue(buttons.size == 2) { "Expected ConfirmDialog to have 2 action buttons (confirm, cancel), found ${buttons.size}" }
                buttons[0] { click() } // confirm is first, see ConfirmDialog.kt
            }
            // ConfirmDialog's dismiss is deferred behind a close animation (see DIALOG_ANIMATION_MS),
            // so unlike the other three dialogs this can't rely on waitForComposeIdle alone.
            waitFor { _ -> layerCount == 1 }
            assertTrue(confirmed.get()) { "Expected ConfirmDialog's onConfirm to fire" }

            // Cancel path.
            triggers[3] { click() } // "Open Confirm" again
            waitFor { _ -> layerCount == 2 }
            node("Surface", layer = LayerSelector.Top) {
                val buttons = nodes("Button")
                buttons[1] { click() } // cancel is second
            }
            waitFor { _ -> layerCount == 1 }
            assertTrue(cancelled.get()) { "Expected ConfirmDialog's onCancel to fire" }
        }
    }
}

private class ModalComponentsProbeScreen(
    private val onAlertConfirm: () -> Unit = {},
    private val onPromptConfirm: (String) -> Unit = {},
    private val onChoiceSelected: (String) -> Unit = {},
    private val onConfirmDialogConfirm: () -> Unit = {},
    private val onConfirmDialogCancel: () -> Unit = {},
) : ComposeScreen(Component.literal("Modal Components Probe")) {
    override fun init() {
        super.init()
        start {
            val layers = LocalLayerManager.current
            Column {
                Button(onClick = {
                    layers.alertDialog(
                        title = Component.literal("Alert"),
                        message = Component.literal("Something happened."),
                        onConfirm = onAlertConfirm,
                    )
                }) { Text(Component.literal("Open Alert"), dropShadow = false) }

                Button(onClick = {
                    layers.promptDialog(
                        title = Component.literal("Prompt"),
                        initialValue = "",
                        validator = { it.isNotBlank() },
                        onConfirm = onPromptConfirm,
                    )
                }) { Text(Component.literal("Open Prompt"), dropShadow = false) }

                Button(onClick = {
                    layers.choiceDialog(
                        title = Component.literal("Choice"),
                        choices = listOf(
                            ModalChoice("alpha", Component.literal("Alpha")),
                            ModalChoice("beta", Component.literal("Beta")),
                            ModalChoice("locked", Component.literal("Locked"), enabled = false),
                        ),
                        onSelected = onChoiceSelected,
                    )
                }) { Text(Component.literal("Open Choice"), dropShadow = false) }

                Button(onClick = {
                    layers.confirmDialog(
                        title = Component.literal("Confirm"),
                        onConfirm = onConfirmDialogConfirm,
                        onCancel = onConfirmDialogCancel,
                    ) {
                        Text(Component.literal("Are you sure?"), dropShadow = false)
                    }
                }) { Text(Component.literal("Open Confirm"), dropShadow = false) }
            }
        }
    }
}
