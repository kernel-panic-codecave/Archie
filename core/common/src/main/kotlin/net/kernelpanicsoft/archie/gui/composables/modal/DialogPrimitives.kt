package net.kernelpanicsoft.archie.gui.composables.modal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import net.kernelpanicsoft.archie.gui.composables.basic.Text
import net.kernelpanicsoft.archie.gui.composables.containers.Surface
import net.kernelpanicsoft.archie.gui.composables.input.Button
import net.kernelpanicsoft.archie.gui.composables.input.textfield.BasicTextField
import net.kernelpanicsoft.archie.gui.layer.ModalScope
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.Arrangement
import net.kernelpanicsoft.archie.gui.layout.Column
import net.kernelpanicsoft.archie.gui.layout.Row
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.position.margin
import net.kernelpanicsoft.archie.gui.modifiers.position.padding
import net.kernelpanicsoft.archie.gui.modifiers.sizeIn
import net.kernelpanicsoft.archie.gui.modifiers.width
import net.kernelpanicsoft.archie.gui.theme.LocalTheme
import net.minecraft.network.chat.Component

/** Value-label pair used by [ChoiceDialog]. */
data class ModalChoice<T>(
    val value: T,
    val label: Component,
    val enabled: Boolean = true,
)

/** Shared [Surface] layout (title, body, bottom action row) used by all built-in dialog composables. */
@Composable
private fun ModalDialogScaffold(
    title: Component,
    modifier: Modifier = Modifier,
    body: @Composable () -> Unit,
    actions: @Composable () -> Unit,
) {
    // Padding on the Surface itself, not margin on the inner Column, matching ConfirmDialog -
    // margin only grows the parent, it doesn't shrink what content measures against.
    Surface(modifier = Modifier.padding(4).then(modifier)) {
        Column(verticalArrangement = Arrangement.spacedBy(4)) {
            Text(
                text = title,
                color = LocalTheme.current.darkTextColor,
                dropShadow = false,
            )
            body()
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.margin(top = 4),
            ) { actions() }
        }
    }
}

/**
 * Simple one-action modal for acknowledgements and warnings.
 *
 * @param title       The dialog's header text.
 * @param message     The body text explaining the alert.
 * @param confirmText Label for the single dismiss button.
 * @param onConfirm   Called just before the modal dismisses itself.
 */
@Composable
fun ModalScope.AlertDialog(
    title: Component,
    message: Component,
    confirmText: Component = Component.literal("OK"),
    onConfirm: () -> Unit = {},
) {
    ModalDialogScaffold(
        title = title,
        modifier = Modifier.sizeIn(minWidth = 150, minHeight = 60),
        body = {
            Text(text = message, dropShadow = false, color = LocalTheme.current.darkTextColor)
        },
        actions = {
            Button(
                onClick = {
                    onConfirm()
                    dismiss()
                },
            ) {
                Text(confirmText, dropShadow = false)
            }
        },
    )
}

/**
 * Input modal with an inline text field and explicit confirm/cancel actions.
 *
 * @param title        The dialog's header text.
 * @param initialValue The text field's starting value.
 * @param prompt       Label text shown above the text field.
 * @param confirmText  Label for the confirm button.
 * @param cancelText   Label for the cancel button.
 * @param validator    The confirm button is only enabled while this returns `true` for the
 *   current field value.
 * @param onConfirm    Called with the field's value just before the modal dismisses itself.
 * @param onCancel     Called just before the modal dismisses itself via the cancel button.
 */
@Composable
fun ModalScope.PromptDialog(
    title: Component,
    initialValue: String = "",
    prompt: Component = Component.literal("Enter a value:"),
    confirmText: Component = Component.literal("Confirm"),
    cancelText: Component = Component.literal("Cancel"),
    validator: (String) -> Boolean = { true },
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit = {},
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }

    ModalDialogScaffold(
        title = title,
        modifier = Modifier.sizeIn(minWidth = 180, minHeight = 80),
        body = {
            Column(verticalArrangement = Arrangement.spacedBy(3)) {
                Text(text = prompt, dropShadow = false, color = LocalTheme.current.darkTextColor)
                BasicTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.width(150),
                )
            }
        },
        actions = {
            Button(
                onClick = {
                    onCancel()
                    dismiss()
                },
            ) {
                Text(cancelText, dropShadow = false)
            }
            Button(
                enabled = validator(value),
                onClick = {
                    onConfirm(value)
                    dismiss()
                },
            ) {
                Text(confirmText, dropShadow = false)
            }
        },
    )
}

/**
 * Multi-choice modal that maps each option in [choices] to its own button, plus one cancel
 * action.
 *
 * @param title      The dialog's header text.
 * @param message    Optional body text shown above the choice buttons.
 * @param choices    The selectable options, one button each, in order.
 * @param cancelText Label for the cancel button.
 * @param onSelected Called with the chosen value just before the modal dismisses itself.
 * @param onCancel   Called just before the modal dismisses itself via the cancel button.
 */
@Composable
fun <T> ModalScope.ChoiceDialog(
    title: Component,
    message: Component? = null,
    choices: List<ModalChoice<T>>,
    cancelText: Component = Component.literal("Cancel"),
    onSelected: (T) -> Unit,
    onCancel: () -> Unit = {},
) {
    ModalDialogScaffold(
        title = title,
        modifier = Modifier.sizeIn(minWidth = 170, minHeight = 70),
        body = {
            Column(verticalArrangement = Arrangement.spacedBy(3)) {
                if (message != null) {
                    Text(text = message, dropShadow = false, color = LocalTheme.current.darkTextColor)
                }
                Column(verticalArrangement = Arrangement.spacedBy(2)) {
                    choices.forEach { choice ->
                        Button(
                            enabled = choice.enabled,
                            modifier = Modifier.width(150),
                            onClick = {
                                onSelected(choice.value)
                                dismiss()
                            },
                        ) {
                            Text(choice.label, dropShadow = false)
                        }
                    }
                }
            }
        },
        actions = {
            Button(
                onClick = {
                    onCancel()
                    dismiss()
                },
            ) {
                Text(cancelText, dropShadow = false)
            }
        },
    )
}


