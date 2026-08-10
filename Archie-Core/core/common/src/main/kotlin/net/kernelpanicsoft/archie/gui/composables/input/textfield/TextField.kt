package net.kernelpanicsoft.archie.gui.composables.input.textfield

import androidx.compose.runtime.*
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.layout.MeasureResult
import net.kernelpanicsoft.archie.gui.layout.Renderer
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.nodes.UINode
import net.kernelpanicsoft.archie.gui.util.KColor
import net.kernelpanicsoft.archie.gui.util.extension.invoke
import net.kernelpanicsoft.archie.gui.util.extension.pose
import net.kernelpanicsoft.archie.gui.util.extension.scissor
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import kotlin.math.max
import kotlin.math.min

private val TEXT_FIELD_SPRITE     = ResourceLocation.withDefaultNamespace("widget/text_field")
private val TEXT_FIELD_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("widget/text_field_highlighted")
private val SCROLLER_SPRITE       = ResourceLocation.withDefaultNamespace("widget/scroller")
private const val BORDER_PADDING  = 4
private const val SCROLL_BAR_W    = 8

/**
 * A simple, controlled text field that uses a plain `String` as its state.
 *
 * This is a convenience wrapper around [TextField] that manages a [TextFieldValue]
 * internally, converting to and from `String` for the [onValueChange] callback.
 *
 * @param value         The current text string.
 * @param onValueChange Called with the updated string on every edit.
 * @param modifier      Additional modifiers applied to the text field.
 * @param enabled       When `false`, input is ignored and the field appears disabled.
 * @param readOnly      When `true`, text can be selected and copied but not edited.
 * @param textColor     ARGB colour of the rendered text.
 * @param cursorColor   ARGB colour of the blinking cursor line.
 * @param selectionColor ARGB colour of the text-selection highlight.
 * @param font          The [Font] used for rendering and measurement.
 * @param singleLine    When `true` the field occupies a single horizontal line.
 * @param maxLength     Maximum allowed character count.
 * @param maxLines      Maximum allowed line count (multi-line only).
 */
@Composable
fun BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textColor: KColor = KColor.ofRgb(0xE0E0E0),
    cursorColor: KColor = KColor.ofRgb(0xFFD0D0D0.toInt()),
    selectionColor: KColor = KColor.ofRgb(-16776961),
    font: Font = Minecraft.getInstance().font,
    singleLine: Boolean = true,
    maxLength: Int = Int.MAX_VALUE,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
) {
    var tfv by remember(value) { mutableStateOf(TextFieldValue(value)) }
    TextField(
        value = tfv,
        onValueChange = { tfv = it; onValueChange(it.text) },
        modifier = modifier, enabled = enabled, readOnly = readOnly,
        textColor = textColor, cursorColor = cursorColor, selectionColor = selectionColor,
        font = font, singleLine = singleLine, maxLength = maxLength, maxLines = maxLines,
    )
}

/**
 * A fully controlled text field composable with the default Minecraft widget appearance.
 *
 * Supports single-line and multi-line modes, cursor navigation, text selection,
 * clipboard operations, and an optional scrollbar for multi-line overflow.
 *
 * Use [BasicTextField] if you only need a simple `String`-based API. Use this composable
 * when you need full control over [TextFieldValue] (e.g. selection or IME state).
 *
 * @param value          The current [TextFieldValue].
 * @param onValueChange  Called on every edit with the new [TextFieldValue].
 * @param modifier       Additional modifiers.
 * @param enabled        Whether the field accepts input.
 * @param readOnly       Whether the field permits editing.
 * @param textColor      Text colour.
 * @param cursorColor    Cursor colour.
 * @param selectionColor Selection highlight colour.
 * @param font           The [Font] used for rendering.
 * @param singleLine     Single vs multi-line mode.
 * @param maxLength      Character cap.
 * @param maxLines       Line cap (multi-line only).
 */
@Composable
fun TextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textColor: KColor = KColor.ofRgb(0xE0E0E0),
    cursorColor: KColor = KColor.ofRgb(0xFFD0D0D0.toInt()),
    selectionColor: KColor = KColor.ofRgb(-16776961),
    font: Font = Minecraft.getInstance().font,
    singleLine: Boolean = true,
    maxLength: Int = Int.MAX_VALUE,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
) {
    TextFieldCore(
        value = value, onValueChange = onValueChange, font = font,
        modifier = modifier, enabled = enabled, readOnly = readOnly,
        singleLine = singleLine, maxLength = maxLength, maxLines = maxLines,
    ) { state ->
        Layout(
            name = "TextField",
            measurePolicy = { _, _, constraints ->
                val w = constraints.maxWidth
                val h = if (singleLine) font.lineHeight + BORDER_PADDING * 2 else constraints.maxHeight
                MeasureResult(w, h) {}
            },
            renderer = object : Renderer {
                override fun render(node: UINode, x: Int, y: Int, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) = guiGraphics {
                    val (w, h) = state.layoutInfo
                    if (w <= 0 || h <= 0) return@guiGraphics

                    val sprite = if (enabled && state.isFocused) TEXT_FIELD_HIGHLIGHTED else TEXT_FIELD_SPRITE
                    blitSprite(sprite, x, y, w, h)

                    val cw = w - BORDER_PADDING * 2
                    val ch = h - BORDER_PADDING * 2
                    val cx = x + BORDER_PADDING
                    val cy = y + BORDER_PADDING

                    scissor(cx, cy, cx + cw, cy + ch) {
                        pose {
                            translate(cx.toDouble(), cy.toDouble(), 0.0)

                            if (singleLine) renderSingleLine(
                                value, font, state, cw, textColor.argb,
                                if (state.showCursor && state.isFocused) cursorColor.argb else 0, selectionColor.argb
                            )
                            else
                            {
                                translate(0.0, -state.scrollY, 0.0)
                                renderMultiLine(
                                    value,
                                    font,
                                    value.text.lines(),
                                    if (state.showCursor && state.isFocused) cursorColor.argb else 0,
                                    selectionColor.argb,
                                    textColor.argb
                                )
                            }
                        }
                    }

                    if (!singleLine) {
                        val contentH = value.text.lines().size * font.lineHeight
                        if (contentH > ch) renderScrollBar(x + w - SCROLL_BAR_W, y, h, contentH, state.scrollY)
                    }
                }
            },
        )
    }
}

// ── Rendering helpers ──────────────────────────────────────────────────────

private fun GuiGraphics.renderSingleLine(value: TextFieldValue, font: Font, state: TextFieldState, width: Int, tc: Int, cc: Int, sc: Int) {
    val text = value.text; val sel = value.selection
    val visible = font.plainSubstrByWidth(text.substring(state.displayPos), width)
    drawString(font, visible, 0, 0, tc)
    if (sel.length > 0) {
        val s = (sel.min - state.displayPos).coerceAtLeast(0)
        val e = (sel.max - state.displayPos).coerceAtLeast(0)
        val vp = text.substring(state.displayPos)
        val sx = font.width(vp.take(s.coerceAtMost(vp.length)))
        val ex = font.width(vp.take(e.coerceAtMost(vp.length)))
        fill(RenderType.guiTextHighlight(), sx, -1, ex, font.lineHeight, sc)
    }
    if (cc != 0 && sel.isCollapsed && sel.start >= state.displayPos) {
        val cx = font.width(text.substring(state.displayPos, sel.start))
        fill(cx, -1, cx + 1, font.lineHeight, cc)
    }
}

private fun GuiGraphics.renderMultiLine(value: TextFieldValue, font: Font, lines: List<String>, cc: Int, sc: Int, tc: Int) {
    val text = value.text; val sel = value.selection
    var y = 0; var charIdx = 0
    for (line in lines) {
        drawString(font, line, 0, y, tc)
        if (sel.length > 0) {
            val ls = charIdx; val le = ls + line.length
            if (sel.min <= le && sel.max >= ls) {
                val sil = max(sel.min, ls) - ls; val eil = min(sel.max, le) - ls
                val sx = font.width(line.take(sil)); val ex = font.width(line.take(eil))
                fill(RenderType.guiTextHighlight(), sx, y, ex, y + font.lineHeight, sc)
            }
        }
        y += font.lineHeight; charIdx += line.length + 1
    }
    if (cc != 0 && sel.isCollapsed) {
        val before = text.take(sel.start)
        val li = before.count { it == '\n' }
        val nl = before.lastIndexOf('\n')
        val col = sel.start - (if (nl == -1) 0 else nl + 1)
        if (li < lines.size) {
            val curX = font.width(lines[li].substring(0, col.coerceAtMost(lines[li].length)))
            val curY = li * font.lineHeight
            fill(curX, curY, curX + 1, curY + font.lineHeight, cc)
        }
    }
}

private fun GuiGraphics.renderScrollBar(x: Int, y: Int, nodeH: Int, contentH: Int, scrollY: Double) {
    val innerH = nodeH - BORDER_PADDING * 2
    val thumbH = Mth.clamp((innerH * innerH) / contentH, 32, innerH)
    val maxScroll = (contentH - innerH).coerceAtLeast(1)
    val sby = y + BORDER_PADDING + Mth.clamp((scrollY * (innerH - thumbH)) / maxScroll, 0.0, (innerH - thumbH).toDouble()).toInt()
    blitSprite(SCROLLER_SPRITE, x, sby, SCROLL_BAR_W, thumbH)
}
