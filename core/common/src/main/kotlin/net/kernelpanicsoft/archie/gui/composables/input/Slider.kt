package net.kernelpanicsoft.archie.gui.composables.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import net.kernelpanicsoft.archie.gui.interaction.DragInteraction
import net.kernelpanicsoft.archie.gui.interaction.MutableInteractionSource
import net.kernelpanicsoft.archie.gui.interaction.collectIsFocusedAsState
import net.kernelpanicsoft.archie.gui.interaction.collectIsHoveredAsState
import net.kernelpanicsoft.archie.gui.layout.Alignment
import net.kernelpanicsoft.archie.gui.layout.BoxMeasurePolicy
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.layout.Renderer
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.input.PointerEventType
import net.kernelpanicsoft.archie.gui.modifiers.input.focusable
import net.kernelpanicsoft.archie.gui.modifiers.input.hoverable
import net.kernelpanicsoft.archie.gui.modifiers.input.onDrag
import net.kernelpanicsoft.archie.gui.modifiers.input.onKeyEvent
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
import org.lwjgl.glfw.GLFW
import kotlin.math.roundToInt

private const val SLIDER_MIN_WIDTH = 96
private const val SLIDER_MIN_HEIGHT = 20
private const val SLIDER_THUMB_WIDTH = 8
private const val SLIDER_THUMB_HEIGHT = 20
private const val SLIDER_TRACK_HEIGHT = 2

/** Fraction of the full 0f..1f range one arrow-key press moves, for a continuous (steps == 0) slider. */
private const val KEYBOARD_STEP_FALLBACK = 0.05f

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

private fun resolveSliderStateName(theme: ComposableTheme, variant: String, enabled: Boolean, hovered: Boolean, dragging: Boolean, focused: Boolean): String =
    WidgetState.resolve(theme, variant, WidgetState.clicked(dragging), WidgetState.focused(hovered || focused), enabled = enabled)

/**
 * Low-level unstyled slider behavior: drag/click-to-position, hover/drag/focus state tracking,
 * with no visuals of its own.
 *
 * A vanilla keyboard/controller focus-navigation stop - unlike the simple toggle inputs
 * ([Checkbox], [Switch], [RadioButton]), a slider has no single "activate" action, so instead
 * of [Clickable]'s Enter/Space it responds to Left/Right arrow keys while focused, nudging the
 * value by one [steps] increment (or [KEYBOARD_STEP_FALLBACK] when continuous).
 *
 * @param value                 The current value, normalized/snapped via [snapSliderValue].
 * @param onValueChange         Called with the new normalized value on every drag/click/arrow-key update.
 * @param modifier              Additional modifiers applied to the outer container.
 * @param enabled               When `false`, pointer and arrow-key events are ignored and this
 *   drops out of the vanilla focus graph entirely.
 * @param steps                 Number of discrete increments to snap to; `0` means continuous.
 * @param onValueChangeFinished Called once when a drag or arrow-key interaction ends.
 * @param content               The visual content; receives hover/drag/focus state and the
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
    content: @Composable (isHovered: Boolean, isDragging: Boolean, isFocused: Boolean, normalizedValue: Float) -> Unit,
) {
    val normalizedValue = snapSliderValue(value, steps)

    // `dragging` stays a plain local var (rather than collectIsDraggedAsState()) since onDrag
    // below needs to synchronously tell "a drag that started on this slider" apart from a
    // stray onDrag call, not just report state for rendering.
    var dragging by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    fun updateFromPointer(node: UINode, mouseX: Double) {
        val localX = (mouseX - node.x).toFloat()
        val fraction = if (node.width <= 1) 0f else localX / node.width.toFloat()
        onValueChange(snapSliderValue(fraction, steps))
    }

    Layout(
        name = "SliderCore",
        measurePolicy = remember { BoxMeasurePolicy(Alignment.CenterStart) },
        modifier = Modifier
            .focusable(enabled = enabled, interactionSource = interactionSource)
            .onKeyEvent { _, event ->
                if (!enabled || !isFocused) return@onKeyEvent
                val step = if (steps > 0) 1f / steps else KEYBOARD_STEP_FALLBACK
                val delta = when (event.keyCode) {
                    GLFW.GLFW_KEY_LEFT -> -step
                    GLFW.GLFW_KEY_RIGHT -> step
                    else -> return@onKeyEvent
                }
                onValueChange(snapSliderValue(normalizedValue + delta, steps))
                onValueChangeFinished()
                event.consume(bypassSuperCall = true)
            }
            .hoverable(interactionSource, enabled = enabled)
            // Not Modifier.draggable(): that reports only a delta per movement, but pressing
            // anywhere on the track needs to jump straight to that absolute position - the same
            // reason Compose Foundation's own Slider doesn't build on plain draggable either.
            .onPointerEvent<UINode>(PointerEventType.PRESS) { node, event ->
                if (!enabled) return@onPointerEvent
                dragging = true
                interactionSource.tryEmit(DragInteraction.Start)
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
                interactionSource.tryEmit(DragInteraction.Stop)
                onValueChangeFinished()
            }
            .then(modifier),
    ) {
        content(hovered, dragging, isFocused, normalizedValue)
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
    ) { hovered, dragging, focused, normalizedValue ->
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

                    val stateName = resolveSliderStateName(trackTheme, variant, enabled, hovered, dragging, focused)
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



