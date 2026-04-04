package net.kernelpanicsoft.archie.gui.composables.input

import androidx.compose.runtime.*
import net.kernelpanicsoft.archie.gui.layout.*
import net.kernelpanicsoft.archie.gui.modifiers.Constraints
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.input.*
import net.kernelpanicsoft.archie.gui.nodes.AUINode
import net.kernelpanicsoft.archie.gui.util.HsvColor
import net.kernelpanicsoft.archie.gui.util.KColor
import net.kernelpanicsoft.archie.gui.util.extension.drawRectOutline
import net.kernelpanicsoft.archie.gui.util.extension.fillGradient
import net.minecraft.client.gui.GuiGraphics
import kotlin.math.max

// ── Internal sub-composables ──────────────────────────────────────────────

@Composable
private fun SaturationValueArea(
    modifier: Modifier = Modifier,
    hue: Float,
    saturation: Float,
    value: Float,
    onSaturationValueChanged: (saturation: Float, value: Float) -> Unit,
) {
    val onEvent = { node: LayoutNode, event: PointerEvent ->
        val newSat = ((event.mouseX - node.absoluteCoords.x) / node.width).toFloat().coerceIn(0f, 1f)
        val newVal = (1f - ((event.mouseY - node.absoluteCoords.y) / node.height).toFloat()).coerceIn(0f, 1f)
        onSaturationValueChanged(newSat, newVal)
        event.consume()
    }

    Layout(
        name = "SaturationValueArea",
        measurePolicy = { _, _, constraints -> MeasureResult(constraints.minWidth, constraints.minHeight) {} },
        renderer = object : Renderer {
            override fun render(node: AUINode, x: Int, y: Int, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
                guiGraphics.fillGradient(x, y, node.width, node.height,
                    KColor.ofHsv(hue, 0f, 1f).argb, KColor.ofHsv(hue, 1f, 1f).argb,
                    KColor.ofHsv(hue, 0f, 0f).argb, KColor.ofHsv(hue, 1f, 0f).argb)
                guiGraphics.drawRectOutline(x + (saturation * node.width).toInt() - 2, y + ((1 - value) * node.height).toInt() - 2, 4, 4, KColor.WHITE.argb)
            }
        },
        modifier = modifier
            .onPointerEvent(PointerEventType.PRESS, onEvent)
            .onDrag(onDragEvent = onEvent),
    )
}

@Composable
private fun HueBar(modifier: Modifier = Modifier, hue: Float, onHueChanged: (Float) -> Unit) {
    val onEvent = { node: LayoutNode, event: PointerEvent ->
        val newHue = (1f - ((event.mouseY - node.absoluteCoords.y) / node.height).toFloat()).coerceIn(0f, 1f)
        onHueChanged(newHue); event.consume()
    }
    Layout(
        name = "HueBar",
        measurePolicy = { _, _, constraints -> MeasureResult(constraints.minWidth, constraints.minHeight) {} },
        renderer = object : Renderer {
            override fun render(node: AUINode, x: Int, y: Int, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
                for (j in 0 until node.height) {
                    guiGraphics.fill(x, y + j, x + node.width, y + j + 1, KColor.ofHsv(1f - (j.toFloat() / node.height), 1f, 1f).argb)
                }
                guiGraphics.drawRectOutline(x - 1, y + ((1 - hue) * node.height).toInt() - 1, node.width + 2, 3, KColor.WHITE.argb)
            }
        },
        modifier = modifier.onPointerEvent(PointerEventType.PRESS, onEvent).onDrag(onDragEvent = onEvent),
    )
}

@Composable
private fun AlphaBar(modifier: Modifier = Modifier, color: HsvColor, onAlphaChanged: (Float) -> Unit) {
    val onEvent = { node: LayoutNode, event: PointerEvent ->
        val newAlpha = ((event.mouseX - node.absoluteCoords.x) / node.width).toFloat().coerceIn(0f, 1f)
        onAlphaChanged(newAlpha); event.consume()
    }
    Layout(
        name = "AlphaBar",
        measurePolicy = { _, _, constraints -> MeasureResult(constraints.minWidth, constraints.minHeight) {} },
        renderer = object : Renderer {
            override fun render(node: AUINode, x: Int, y: Int, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
                val checkerSize = 4
                for (cx in 0 until node.width step checkerSize)
                    for (cy in 0 until node.height step checkerSize)
                        guiGraphics.fill(x + cx, y + cy, x + cx + checkerSize, y + cy + checkerSize,
                            if ((cx / checkerSize + cy / checkerSize) % 2 == 0) KColor.WHITE.rgb else KColor.LIGHT_GRAY.rgb)
                val opaque = color.copy(alpha = 1f).toKColor().argb
                val transparent = color.copy(alpha = 0f).toKColor().argb
                guiGraphics.fillGradient(x, y, node.width, node.height, transparent, opaque, transparent, opaque)
                guiGraphics.drawRectOutline(x + (color.alpha * node.width).toInt() - 1, y - 1, 3, node.height + 2, KColor.WHITE.argb)
            }
        },
        modifier = modifier.onPointerEvent(PointerEventType.PRESS, onEvent).onDrag(onDragEvent = onEvent),
    )
}

// ── Public API ─────────────────────────────────────────────────────────────

/**
 * A fully controlled HSV + alpha colour picker composable.
 *
 * This composable is **stateless**: it displays the colour provided by [color] and
 * reports changes through [onColorChanged]. The caller is responsible for creating
 * and hoisting the state, typically via `remember { mutableStateOf(HsvColor(...)) }`.
 *
 * The picker consists of a saturation-value gradient area, an optional alpha slider, and
 * a vertical hue slider. Apply a `Modifier.size(width, height)` to set the picker's
 * overall dimensions.
 *
 * ### Example
 * ```kotlin
 * var color by remember { mutableStateOf(HsvColor.from(KColor.RED)) }
 * ColorPicker(
 *     color = color,
 *     modifier = Modifier.size(200, 150),
 *     onColorChanged = { color = it },
 * )
 * ```
 *
 * @param color            The current colour value to display.
 * @param showAlphaBar     Whether to show the horizontal alpha slider.
 * @param alphaBarHeight   Height of the alpha slider in pixels.
 * @param hueBarWidth      Width of the vertical hue slider in pixels.
 * @param barPadding       Gap in pixels between the main SV area and the sliders.
 * @param modifier         Modifiers applied to the picker container (size required).
 * @param onColorChanged   Called with the updated [HsvColor] on every user interaction.
 */
@Composable
fun ColorPicker(
    color: HsvColor,
    showAlphaBar: Boolean = true,
    alphaBarHeight: Int = 12,
    hueBarWidth: Int = 16,
    barPadding: Int = 8,
    modifier: Modifier = Modifier,
    onColorChanged: (HsvColor) -> Unit,
) {
    val updatedCallback by rememberUpdatedState(onColorChanged)

    val measurePolicy = remember(showAlphaBar, alphaBarHeight, hueBarWidth, barPadding) {
        MeasurePolicy { _, measurables, constraints ->
            if (showAlphaBar) {
                check(measurables.size == 3) { "ColorPicker with showAlphaBar=true expects exactly 3 children" }
                val (svM, alphaM, hueM) = measurables
                val svW = max(0, constraints.maxWidth - hueBarWidth - barPadding)
                val svH = max(0, constraints.maxHeight - alphaBarHeight - barPadding)
                val svP    = svM.measure(Constraints(svW, svW, svH, svH))
                val alphaP = alphaM.measure(Constraints(svW, svW, alphaBarHeight, alphaBarHeight))
                val hueP   = hueM.measure(Constraints(hueBarWidth, hueBarWidth, constraints.maxHeight, constraints.maxHeight))
                MeasureResult(constraints.maxWidth, constraints.maxHeight) {
                    svP.placeAt(0, 0)
                    alphaP.placeAt(0, svP.height + barPadding)
                    hueP.placeAt(svP.width + barPadding, 0)
                }
            } else {
                check(measurables.size == 2) { "ColorPicker with showAlphaBar=false expects exactly 2 children" }
                val (svM, hueM) = measurables
                val svW = max(0, constraints.maxWidth - hueBarWidth - barPadding)
                val svP  = svM.measure(Constraints(svW, svW, constraints.maxHeight, constraints.maxHeight))
                val hueP = hueM.measure(Constraints(hueBarWidth, hueBarWidth, constraints.maxHeight, constraints.maxHeight))
                MeasureResult(constraints.maxWidth, constraints.maxHeight) {
                    svP.placeAt(0, 0)
                    hueP.placeAt(svP.width + barPadding, 0)
                }
            }
        }
    }

    Layout(name = "ColorPicker", measurePolicy = measurePolicy, modifier = modifier) {
        SaturationValueArea(hue = color.hue, saturation = color.saturation, value = color.value) { s, v ->
            updatedCallback(color.copy(saturation = s, value = v))
        }
        if (showAlphaBar) {
            AlphaBar(color = color) { a -> updatedCallback(color.copy(alpha = a)) }
        }
        HueBar(hue = color.hue) { h -> updatedCallback(color.copy(hue = h)) }
    }
}
