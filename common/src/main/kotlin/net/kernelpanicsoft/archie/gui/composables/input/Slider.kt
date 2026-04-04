package net.kernelpanicsoft.archie.gui.composables.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.BoxMeasurePolicy
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.layout.Renderer
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.input.PointerEventType
import net.kernelpanicsoft.archie.gui.modifiers.input.onDrag
import net.kernelpanicsoft.archie.gui.modifiers.input.onPointerEvent
import net.kernelpanicsoft.archie.gui.modifiers.sizeIn
import net.kernelpanicsoft.archie.gui.nodes.AUINode
import net.kernelpanicsoft.archie.gui.composables.theme.TextureStates
import net.kernelpanicsoft.archie.gui.theme.LocalTheme
import net.kernelpanicsoft.archie.gui.theme.ThemeVariants
import net.kernelpanicsoft.archie.gui.util.extension.drawThemeState
import net.minecraft.client.gui.GuiGraphics
import kotlin.math.roundToInt

private const val SLIDER_MIN_WIDTH = 96
private const val SLIDER_MIN_HEIGHT = 20
private const val SLIDER_THUMB_WIDTH = 8
private const val SLIDER_THUMB_HEIGHT = 20
private const val SLIDER_TRACK_HEIGHT = 2

internal fun normalizeSliderValue(value: Float): Float = value.coerceIn(0f, 1f)

internal fun snapSliderValue(value: Float, steps: Int): Float {
    if (steps <= 0) return normalizeSliderValue(value)
    val clamped = normalizeSliderValue(value)
    val stepSize = 1f / steps.toFloat()
    return (clamped / stepSize).roundToInt() * stepSize
}

internal fun resolveSliderThumbX(rawThumbX: Int, sliderX: Int, sliderWidth: Int, thumbWidth: Int = SLIDER_THUMB_WIDTH): Int {
    val minThumbX = sliderX
    val maxThumbX = (sliderX + sliderWidth - thumbWidth).coerceAtLeast(minThumbX)
    return rawThumbX.coerceIn(minThumbX, maxThumbX)
}

private fun resolveSliderStateName(enabled: Boolean, hovered: Boolean, dragging: Boolean): String = when {
    !enabled -> TextureStates.DISABLED
    dragging -> TextureStates.CLICKED
    hovered -> TextureStates.HOVERED
    else -> TextureStates.DEFAULT
}

@Composable
fun SliderCore(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    steps: Int = 0,
    onValueChangeFinished: () -> Unit = {},
    content: @Composable (isHovered: Boolean, isDragging: Boolean, normalizedValue: Float) -> Unit,
) {
    val normalizedValue = snapSliderValue(value, steps)

    var hovered by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }

    fun updateFromPointer(node: AUINode, mouseX: Double) {
        val localX = (mouseX - node.x).toFloat()
        val fraction = if (node.width <= 1) 0f else localX / node.width.toFloat()
        onValueChange(snapSliderValue(fraction, steps))
    }

    Layout(
        name = "SliderCore",
        measurePolicy = remember { BoxMeasurePolicy(Alignment.CenterStart) },
        modifier = Modifier
            .onPointerEvent<AUINode>(PointerEventType.ENTER) { _, event ->
                if (!enabled) return@onPointerEvent
                hovered = true
                event.consume()
            }
            .onPointerEvent<AUINode>(PointerEventType.EXIT) { _, event ->
                hovered = false
                dragging = false
                if (enabled) event.consume()
            }
            .onPointerEvent<AUINode>(PointerEventType.PRESS) { node, event ->
                if (!enabled) return@onPointerEvent
                dragging = true
                updateFromPointer(node, event.mouseX)
                event.consume(true)
            }
            .onDrag<AUINode> { node, event ->
                if (!enabled || !dragging) return@onDrag
                updateFromPointer(node, event.mouseX)
                event.consume()
            }
            .onPointerEvent<AUINode>(PointerEventType.GLOBAL_RELEASE) { _, _ ->
                if (!enabled || !dragging) return@onPointerEvent
                dragging = false
                onValueChangeFinished()
            }
            .then(modifier),
    ) {
        content(hovered, dragging, normalizedValue)
    }
}

@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: String = ThemeVariants.DEFAULT,
    steps: Int = 0,
    onValueChangeFinished: () -> Unit = {},
) {
    val measurePolicy = remember { BoxMeasurePolicy(Alignment.CenterStart) }
    val theme = LocalTheme.current
    val trackTheme = theme.getComposableTheme("slider")
    val thumbTheme = theme.getComposableTheme("slider_handle")
    SliderCore(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        modifier = Modifier
            .sizeIn(minWidth = SLIDER_MIN_WIDTH, minHeight = SLIDER_MIN_HEIGHT)
            .then(modifier),
    ) { hovered, dragging, normalizedValue ->
        Layout(
            name = "Slider",
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
                    val trackY = y + (node.height - SLIDER_TRACK_HEIGHT) / 2
                    val trackStart = x + (SLIDER_THUMB_WIDTH / 2)
                    val trackEnd = x + node.width - (SLIDER_THUMB_WIDTH / 2)
                    val availableTrack = (trackEnd - trackStart).coerceAtLeast(1)
                    val fillEnd = trackStart + (availableTrack * normalizedValue).roundToInt()
                    val thumbX = resolveSliderThumbX(
                        rawThumbX = fillEnd - (SLIDER_THUMB_WIDTH / 2),
                        sliderX = x,
                        sliderWidth = node.width,
                        thumbWidth = SLIDER_THUMB_WIDTH,
                    )
                    val thumbY = y + (node.height - SLIDER_THUMB_HEIGHT) / 2

                    val stateName = resolveSliderStateName(enabled, hovered, dragging)
                    val trackState = trackTheme.getState(stateName, variant)
                    val thumbState = thumbTheme.getState(stateName, variant)

                    val fillColor = if (enabled) 0xFF6BA8FF.toInt() else 0xFF5A5A5A.toInt()

                    guiGraphics.drawThemeState(trackState, x, y, node.width, node.height)
                    guiGraphics.fill(trackStart, trackY, fillEnd, trackY + SLIDER_TRACK_HEIGHT, fillColor)
                    guiGraphics.drawThemeState(thumbState, thumbX, thumbY, SLIDER_THUMB_WIDTH, SLIDER_THUMB_HEIGHT)
                    super.render(node, x, y, guiGraphics, mouseX, mouseY, partialTick)
                }
            },
        )
    }
}



