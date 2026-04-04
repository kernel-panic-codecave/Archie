package net.kernelpanicsoft.archie.gui.composables.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import net.minecraft.client.gui.GuiGraphics
import kotlin.math.roundToInt

private const val SLIDER_MIN_WIDTH = 96
private const val SLIDER_MIN_HEIGHT = 16
private const val SLIDER_THUMB_SIZE = 8

internal fun normalizeSliderValue(value: Float): Float = value.coerceIn(0f, 1f)

internal fun snapSliderValue(value: Float, steps: Int): Float {
    if (steps <= 0) return normalizeSliderValue(value)
    val clamped = normalizeSliderValue(value)
    val stepSize = 1f / steps.toFloat()
    return (clamped / stepSize).roundToInt() * stepSize
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

    var hovered = false
    var dragging = false

    fun updateFromPointer(node: AUINode, mouseX: Double) {
        val localX = (mouseX - node.absoluteCoords.x).toFloat()
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
    steps: Int = 0,
    onValueChangeFinished: () -> Unit = {},
) {
    val measurePolicy = remember { BoxMeasurePolicy(Alignment.CenterStart) }
    SliderCore(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        modifier = Modifier
            .sizeIn(minWidth = SLIDER_MIN_WIDTH, minHeight = SLIDER_MIN_HEIGHT)
            .then(modifier),
    ) { hovered, _, normalizedValue ->
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
                    val trackY = y + (node.height / 2) - 1
                    val trackStart = x + (SLIDER_THUMB_SIZE / 2)
                    val trackEnd = x + node.width - (SLIDER_THUMB_SIZE / 2)
                    val availableTrack = (trackEnd - trackStart).coerceAtLeast(1)
                    val fillEnd = trackStart + (availableTrack * normalizedValue).roundToInt()
                    val thumbX = (fillEnd - (SLIDER_THUMB_SIZE / 2)).coerceIn(x, x + node.width - SLIDER_THUMB_SIZE)
                    val thumbY = y + (node.height - SLIDER_THUMB_SIZE) / 2

                    val baseTrackColor = if (enabled) 0xFF5A5A5A.toInt() else 0xFF404040.toInt()
                    val fillColor = if (enabled) 0xFF6BA8FF.toInt() else 0xFF5A5A5A.toInt()
                    val thumbColor = when {
                        !enabled -> 0xFF8A8A8A.toInt()
                        hovered -> 0xFFFFFFFF.toInt()
                        else -> 0xFFE0E0E0.toInt()
                    }

                    guiGraphics.fill(trackStart, trackY, trackEnd, trackY + 2, baseTrackColor)
                    guiGraphics.fill(trackStart, trackY, fillEnd, trackY + 2, fillColor)
                    guiGraphics.fill(thumbX, thumbY, thumbX + SLIDER_THUMB_SIZE, thumbY + SLIDER_THUMB_SIZE, thumbColor)
                    super.render(node, x, y, guiGraphics, mouseX, mouseY, partialTick)
                }
            },
        )
    }
}

