package net.kernelpanicsoft.archie.gui.composables.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.kernelpanicsoft.archie.gui.animation.AnimationSpec
import net.kernelpanicsoft.archie.gui.animation.Easings
import net.kernelpanicsoft.archie.gui.animation.animateInt
import net.kernelpanicsoft.archie.gui.layout.BoxMeasurePolicy
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.layout.Renderer
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.sizeIn
import net.kernelpanicsoft.archie.gui.nodes.AUINode
import net.kernelpanicsoft.archie.gui.util.KColor
import net.minecraft.client.gui.GuiGraphics

private const val SWITCH_MIN_WIDTH = 34
private const val SWITCH_MIN_HEIGHT = 18
private const val SWITCH_PADDING = 2
private const val SWITCH_THUMB_SIZE = 14

/** Clamps a raw thumb x-offset so the thumb stays within the track, respecting [SWITCH_PADDING]. */
internal fun resolveSwitchThumbOffset(thumbOffset: Int, trackWidth: Int): Int {
    val minOffset = SWITCH_PADDING
    val maxOffset = (trackWidth - SWITCH_THUMB_SIZE - SWITCH_PADDING).coerceAtLeast(minOffset)
    return thumbOffset.coerceIn(minOffset, maxOffset)
}

/**
 * Low-level switch primitive exposing hover/press state and checked state to custom visuals.
 */
@Composable
fun SwitchCore(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable (isHovered: Boolean, isPressed: Boolean, checked: Boolean) -> Unit,
) {
    Clickable(
        onClick = { onCheckedChange(!checked) },
        enabled = enabled,
        modifier = modifier,
    ) { hovered, pressed ->
        content(hovered, pressed, checked)
    }
}

/**
 * Simple styled switch control suitable for toggling boolean settings.
 */
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val measurePolicy = remember { BoxMeasurePolicy(Alignment.CenterStart) }
    val thumbOffset = animateInt(
        targetValue = if (checked) SWITCH_MIN_WIDTH - SWITCH_THUMB_SIZE - SWITCH_PADDING else SWITCH_PADDING,
        spec = AnimationSpec(durationMillis = 140, easing = Easings.OutCubic),
    )

    SwitchCore(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = Modifier
            .sizeIn(minWidth = SWITCH_MIN_WIDTH, minHeight = SWITCH_MIN_HEIGHT)
            .then(modifier),
    ) { hovered, _, currentChecked ->
        Layout(
            name = "Switch",
            measurePolicy = measurePolicy,
            renderer = object : Renderer {
                override fun render(
                    node: AUINode,
                    x: Int,
                    y: Int,
                    guiGraphics: GuiGraphics,
                    mouseX: Int,
                    mouseY: Int,
                    partialTick: Float,
                ) {
                    val trackColor = when {
                        !enabled -> 0xFF5A5A5A.toInt()
                        currentChecked -> 0xFF4CAF50.toInt()
                        hovered -> 0xFF6A6A6A.toInt()
                        else -> 0xFF4A4A4A.toInt()
                    }
                    val thumbColor = if (enabled) KColor.WHITE.argb else 0xFFB0B0B0.toInt()

                    guiGraphics.fill(x, y, x + node.width, y + node.height, trackColor)
                    val thumbX = x + resolveSwitchThumbOffset(thumbOffset, node.width)
                    val thumbY = y + ((node.height - SWITCH_THUMB_SIZE) / 2)
                    guiGraphics.fill(thumbX, thumbY, thumbX + SWITCH_THUMB_SIZE, thumbY + SWITCH_THUMB_SIZE, thumbColor)
                    super.render(node, x, y, guiGraphics, mouseX, mouseY, partialTick)
                }
            },
        )
    }
}

