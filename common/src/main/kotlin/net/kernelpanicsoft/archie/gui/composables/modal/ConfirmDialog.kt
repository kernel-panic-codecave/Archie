package net.kernelpanicsoft.archie.gui.composables.modal

import androidx.compose.runtime.Composable
import net.kernelpanicsoft.archie.gui.composables.basic.Text
import net.kernelpanicsoft.archie.gui.composables.containers.Surface
import net.kernelpanicsoft.archie.gui.composables.input.Button
import net.kernelpanicsoft.archie.gui.layer.ModalScope
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.Arrangement
import net.kernelpanicsoft.archie.gui.layout.Column
import net.kernelpanicsoft.archie.gui.layout.Row
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.position.margin
import net.kernelpanicsoft.archie.gui.modifiers.position.padding
import net.kernelpanicsoft.archie.gui.modifiers.sizeIn
import net.kernelpanicsoft.archie.gui.theme.LocalTheme
import net.minecraft.network.chat.Component

@Composable
fun ModalScope.ConfirmDialog(
	title: Component = Component.literal("Confirm Dialog"),
	confirmText: Component = Component.literal("Confirm"),
	cancelText: Component = Component.literal("Cancel"),
	onConfirm: () -> Unit = {},
	onCancel: () -> Unit = {},
	content: @Composable () -> Unit
)
{
	Surface(modifier = Modifier.padding(4)) {
		Column(modifier = Modifier.margin(4)) {
			Text(
				text = title,
				modifier = Modifier.margin(bottom = 4),
				color = LocalTheme.current.darkTextColor,
				dropShadow = false
			)
			content()
			Row(
				modifier = Modifier.margin(top = 4),
				horizontalArrangement = Arrangement.SpaceEvenly,
				verticalAlignment = Alignment.CenterVertically
			) {
				Button(
					onClick = { onConfirm(); dismiss() },
					modifier = Modifier.sizeIn(minWidth = 50, minHeight = 20)
				) { Text(confirmText) }
				Button(
					onClick = { onCancel(); dismiss() },
					modifier = Modifier.sizeIn(minWidth = 50, minHeight = 20)
				) { Text(cancelText) }
			}
		}
	}
}