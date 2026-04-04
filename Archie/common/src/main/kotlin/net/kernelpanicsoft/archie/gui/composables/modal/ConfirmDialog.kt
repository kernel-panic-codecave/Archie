package net.kernelpanicsoft.archie.gui.composables.modal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import net.kernelpanicsoft.archie.gui.animation.AnimationSpec
import net.kernelpanicsoft.archie.gui.animation.Easings
import net.kernelpanicsoft.archie.gui.animation.animateInt
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
import net.kernelpanicsoft.archie.gui.modifiers.position.offset
import net.kernelpanicsoft.archie.gui.modifiers.position.padding
import net.kernelpanicsoft.archie.gui.modifiers.sizeIn
import net.kernelpanicsoft.archie.gui.theme.LocalTheme
import net.minecraft.network.chat.Component
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val DIALOG_ANIMATION_MS = 180L

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
	val scope = rememberCoroutineScope()
	var entered by remember { mutableStateOf(false) }
	var closing by remember { mutableStateOf(false) }
	LaunchedEffect(Unit) { entered = true }

	fun closeWithAnimation(action: () -> Unit) {
		if (closing) return
		closing = true
		entered = false
		action()
		scope.launch {
			delay(DIALOG_ANIMATION_MS.milliseconds)
			dismiss()
		}
	}

	val offsetY = animateInt(
		targetValue = if (entered) 0 else 8,
		spec = AnimationSpec(durationMillis = DIALOG_ANIMATION_MS.toInt(), easing = Easings.OutCubic),
	)

	Surface(modifier = Modifier.padding(4).offset(x = 0, y = offsetY)) {
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
					onClick = { closeWithAnimation(onConfirm) },
					enabled = !closing,
					modifier = Modifier.sizeIn(minWidth = 50, minHeight = 20)
				) { Text(confirmText) }
				Button(
					onClick = { closeWithAnimation(onCancel) },
					enabled = !closing,
					modifier = Modifier.sizeIn(minWidth = 50, minHeight = 20)
				) { Text(cancelText) }
			}
		}
	}
}