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
import net.kernelpanicsoft.archie.gui.nodes.UINode
import net.kernelpanicsoft.archie.gui.composables.theme.WidgetState
import net.kernelpanicsoft.archie.gui.theme.ComposableTheme
import net.kernelpanicsoft.archie.gui.theme.LocalTheme
import net.kernelpanicsoft.archie.gui.theme.ThemeVariants
import net.kernelpanicsoft.archie.gui.util.extension.drawThemeState
import net.kernelpanicsoft.archie.gui.util.extension.invoke
import net.minecraft.client.gui.GuiGraphics
import kotlin.math.roundToInt

private const val SLIDER_MIN_WIDTH = 96
private const val SLIDER_MIN_HEIGHT = 20
private const val SLIDER_THUMB_WIDTH = 8
private const val SLIDER_THUMB_HEIGHT = 20
private const val SLIDER_TRACK_HEIGHT = 2

/** Clamps a slider value into the normalized `0f..1f` range. */
internal fun normalizeSliderValue(value: Float): Float = value.coerceIn(0f, 1f)

/** Normalizes [value] then rounds it to the nearest of [steps] evenly spaced increments (no snapping when [steps] <= 0). */
internal fun snapSliderValue(value: Float, steps: Int): Float {
    if (steps <= 0) return normalizeSliderValue(value)
    val clamped = normalizeSliderValue(value)
    val stepSize = 1f / steps.toFloat()
    return (clamped / stepSize).roundToInt() * stepSize
}

/** Clamps a raw thumb x-position so the [thumbWidth]-wide thumb stays within the track bounds. */
internal fun resolveSliderThumbX(rawThumbX: Int, sliderX: Int, sliderWidth: Int, thumbWidth: Int = SLIDER_THUMB_WIDTH): Int {
    val minThumbX = sliderX
    val maxThumbX = (sliderX + sliderWidth - thumbWidth).coerceAtLeast(minThumbX)
    return rawThumbX.coerceIn(minThumbX, maxThumbX)
}

private fun resolveSliderStateName(theme: ComposableTheme, variant: String, enabled: Boolean, hovered: Boolean, dragging: Boolean): String =
    WidgetState.resolve(theme, variant, WidgetState.clicked(dragging), WidgetState.hovered(hovered), enabled = enabled)

/**
 * Low-level unstyled slider behavior: drag/click-to-position and hover/drag state tracking,
 * with no visuals of its own.
 *
 * @param value                 The current value, normalized/snapped via [snapSliderValue].
 * @param onValueChange         Called with the new normalized value on every drag/click update.
 * @param modifier              Additional modifiers applied to the outer container.
 * @param enabled               When `false`, pointer events are ignored.
 * @param steps                 Number of discrete increments to snap to; `0` means continuous.
 * @param onValueChangeFinished Called once when a drag interaction ends (on release).
 * @param content               The visual content; receives hover/drag state and the
 *   normalized, snapped value to render.
 */
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

    fun updateFromPointer(node: UINode, mouseX: Double) {
        val localX = (mouseX - node.x).toFloat()
        val fraction = if (node.width <= 1) 0f else localX / node.width.toFloat()
        onValueChange(snapSliderValue(fraction, steps))
    }

    Layout(
        name = "SliderCore",
        measurePolicy = remember { BoxMeasurePolicy(Alignment.CenterStart) },
        modifier = Modifier
            .onPointerEvent<UINode>(PointerEventType.ENTER) { _, event ->
                if (!enabled) return@onPointerEvent
                hovered = true
                event.consume()
            }
            .onPointerEvent<UINode>(PointerEventType.EXIT) { _, event ->
                hovered = false
                dragging = false
                if (enabled) event.consume()
            }
            .onPointerEvent<UINode>(PointerEventType.PRESS) { node, event ->
                if (!enabled) return@onPointerEvent
                dragging = true
                updateFromPointer(node, event.mouseX)
                event.consume(true)
            }
            .onDrag<UINode> { node, event ->
                if (!enabled || !dragging) return@onDrag
                updateFromPointer(node, event.mouseX)
                event.consume()
            }
            .onPointerEvent<UINode>(PointerEventType.GLOBAL_RELEASE) { _, _ ->
                if (!enabled || !dragging) return@onPointerEvent
                dragging = false
                onValueChangeFinished()
            }
            .then(modifier),
    ) {
        content(hovered, dragging, normalizedValue)
    }
}

/**
 * A standard themed horizontal slider, drawing a "slider" track and "slider_handle" thumb
 * from the current theme, plus a solid-color fill up to the thumb.
 *
 * @param value                 The current value, normalized/snapped via [snapSliderValue].
 * @param onValueChange         Called with the new normalized value on every drag/click update.
 * @param modifier              Additional modifiers applied to the outer container.
 * @param enabled               When `false`, the disabled state is drawn and input is ignored.
 * @param variant               The theme variant used for both the track and thumb textures.
 * @param steps                 Number of discrete increments to snap to; `0` means continuous.
 * @param onValueChangeFinished Called once when a drag interaction ends (on release).
 */
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
    val sizeModifier = Modifier.sizeIn(minWidth = SLIDER_MIN_WIDTH, minHeight = SLIDER_MIN_HEIGHT)
    SliderCore(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        modifier = sizeModifier.then(modifier),
    ) { hovered, dragging, normalizedValue ->
        Layout(
            name = "Slider",
            measurePolicy = measurePolicy,
            modifier = sizeModifier,
            renderer = object : Renderer {
                override fun render(
	                node: UINode,
	                x: Int,
	                y: Int,
	                guiGraphics: GuiGraphics,
	                mouseX: Int,
	                mouseY: Int,
	                partialTick: Float,
                ) = guiGraphics {
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

                    val stateName = resolveSliderStateName(trackTheme, variant, enabled, hovered, dragging)
                    node.renderState = stateName
                    val trackState = trackTheme.getState(stateName, variant)
                    val thumbState = thumbTheme.getState(stateName, variant)

                    val fillColor = if (enabled) 0xFF6BA8FF.toInt() else 0xFF5A5A5A.toInt()

                    drawThemeState(trackState, x, y, node.width, node.height)
                    fill(trackStart, trackY, fillEnd, trackY + SLIDER_TRACK_HEIGHT, fillColor)
                    drawThemeState(thumbState, thumbX, thumbY, SLIDER_THUMB_WIDTH, SLIDER_THUMB_HEIGHT)
                }
            },
        )
    }
}



