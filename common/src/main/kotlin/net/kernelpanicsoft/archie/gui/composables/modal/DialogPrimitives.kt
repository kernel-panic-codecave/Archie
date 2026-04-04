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

@Composable
private fun ModalDialogScaffold(
    title: Component,
    modifier: Modifier = Modifier,
    body: @Composable () -> Unit,
    actions: @Composable () -> Unit,
) {
    Surface(modifier = modifier) {
        Column(modifier = Modifier.margin(4), verticalArrangement = Arrangement.spacedBy(4)) {
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

/** Simple one-action modal for acknowledgements and warnings. */
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
            Button(onClick = {
                onConfirm()
                dismiss()
            }) {
                Text(confirmText, dropShadow = false)
            }
        },
    )
}

/** Input modal with inline text field and explicit confirm/cancel actions. */
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
            Button(onClick = {
                onCancel()
                dismiss()
            }) {
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

/** Multi-choice modal that maps each option to a button action. */
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
            Button(onClick = {
                onCancel()
                dismiss()
            }) {
                Text(cancelText, dropShadow = false)
            }
        },
    )
}


