package net.kernelpanicsoft.archie.gui.composables.input.textfield

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.layout.LayoutNode
import net.kernelpanicsoft.archie.gui.layout.MeasureResult
import net.kernelpanicsoft.archie.gui.layout.Size
import net.kernelpanicsoft.archie.gui.modifiers.Constraints
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.input.*
import net.kernelpanicsoft.archie.util.minecraftClient
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.screens.Screen
import net.minecraft.util.Mth
import net.minecraft.util.StringUtil
import kotlin.math.max

private const val BORDER_PADDING = 4
private const val CURSOR_BLINK_INTERVAL_MS = 300L

/**
 * Internal mutable state for a text field, tracking focus, scroll position, cursor blink,
 * and layout dimensions.
 *
 * Obtain an instance via [rememberTextFieldState] and pass it to [TextFieldCore].
 */
@Stable
class TextFieldState {
    /** Horizontal scroll offset for single-line fields (index of the first visible character). */
    var displayPos by mutableStateOf(0)
    /** Vertical scroll offset for multi-line fields, in pixels. */
    var scrollY by mutableStateOf(0.0)
    /** Whether the field currently holds input focus. */
    var isFocused by mutableStateOf(false)
    /** Whether the blinking cursor is currently visible. */
    var showCursor by mutableStateOf(false)
    internal var lastBlink by mutableStateOf(0L)
    /** Whether the user is dragging the multi-line scroll bar. */
    var isDraggingScrollbar by mutableStateOf(false)
    internal var layoutInfo by mutableStateOf(Size(0, 0))

    /** Updates focus state and resets the cursor blink timer on focus gain. */
    fun onFocusChange(focused: Boolean) {
        if (isFocused != focused) {
            isFocused = focused
            if (focused) { lastBlink = System.currentTimeMillis(); showCursor = true }
            else showCursor = false
        }
    }
}

/** Creates and remembers a [TextFieldState] instance. */
@Composable
fun rememberTextFieldState(): TextFieldState = remember { TextFieldState() }

/**
 * Core composable that handles all state, focus, and input logic for a text field while
 * delegating visual rendering entirely to [content].
 *
 * This is the lowest-level text field building block. Build higher-level components on top
 * of it (as [TextField] and [BasicTextField] do) to add visual decorations.
 *
 * @param value          The current [TextFieldValue].
 * @param onValueChange  Called whenever the user modifies the text or cursor position.
 * @param font           The [Font] used for text measurement.
 * @param modifier       Additional modifiers applied to the invisible layout node.
 * @param enabled        When `false`, keyboard events are ignored.
 * @param readOnly       When `true`, the text can be selected and copied but not edited.
 * @param singleLine     When `true`, Enter inserts a newline; otherwise the field is single-line.
 * @param maxLength      Maximum permitted character count.
 * @param maxLines       Maximum permitted line count (only relevant when [singleLine] is `false`).
 * @param content        The visual content composable; receives the managed [TextFieldState].
 */
@Composable
fun TextFieldCore(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    font: Font,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    maxLength: Int = Int.MAX_VALUE,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    content: @Composable (state: TextFieldState) -> Unit,
) {
    val state = rememberTextFieldState()

    // Cursor blink coroutine
    LaunchedEffect(state.isFocused) {
        if (state.isFocused) {
            while (true) {
                val t = System.currentTimeMillis()
                if (t - state.lastBlink > CURSOR_BLINK_INTERVAL_MS) { state.showCursor = !state.showCursor; state.lastBlink = t }
                delay(50)
            }
        } else state.showCursor = false
    }

    val scrollToCursor = {
        val (nodeWidth, nodeHeight) = state.layoutInfo
        if (nodeWidth > 0 && nodeHeight > 0) {
            if (singleLine) {
                val innerWidth = nodeWidth - BORDER_PADDING * 2
                val visible = font.plainSubstrByWidth(value.text.substring(state.displayPos), innerWidth)
                val endPos = visible.length + state.displayPos
                if (value.selection.start > endPos) state.displayPos = value.selection.start - visible.length
                else if (value.selection.start <= state.displayPos) state.displayPos = value.selection.start
                state.displayPos = Mth.clamp(state.displayPos, 0, value.text.length)
            } else {
                val innerHeight = nodeHeight - BORDER_PADDING * 2
                val contentHeight = value.text.lines().size * font.lineHeight
                val maxScroll = max(0, contentHeight - innerHeight)
                val cursorLine = value.text.take(value.selection.start).count { it == '\n' }
                val cursorY = cursorLine * font.lineHeight
                if (cursorY < state.scrollY) state.scrollY = cursorY.toDouble()
                if (cursorY + font.lineHeight > state.scrollY + innerHeight) state.scrollY = (cursorY + font.lineHeight - innerHeight).toDouble()
                state.scrollY = Mth.clamp(state.scrollY, 0.0, maxScroll.toDouble())
            }
        }
    }

    val onValueChangeAndScroll: (TextFieldValue) -> Unit = { v ->
        onValueChange(v); scrollToCursor(); state.showCursor = true; state.lastBlink = System.currentTimeMillis()
    }

    Layout(
        name = "TextFieldCore",
        measurePolicy = { _, measurables, constraints ->
            val w = constraints.maxWidth
            val h = if (singleLine) font.lineHeight + BORDER_PADDING * 2 else constraints.maxHeight
            state.layoutInfo = Size(w, h)

            val fixedConstraints = Constraints(minWidth = w, maxWidth = w, minHeight = h, maxHeight = h)
            val placeables = measurables.map { it.measure(fixedConstraints) }
            MeasureResult(w, h) {
                placeables.forEach { it.placeAt(0, 0) }
            }
        },
        modifier = modifier
            .onKeyEvent { _, event ->
                if (!enabled || !state.isFocused) return@onKeyEvent
                if (event.keyCode == 256) { state.onFocusChange(false); event.consume(true); return@onKeyEvent }
                var handled = true
                val result = when {
                    Screen.isSelectAll(event.keyCode) -> value.copy(selection = TextRange(0, value.text.length))
                    Screen.isCopy(event.keyCode)  -> { Minecraft.getInstance().keyboardHandler.clipboard = value.selectedText; value }
                    Screen.isPaste(event.keyCode) && !readOnly -> handlePaste(value, maxLength, maxLines, singleLine)
                    Screen.isCut(event.keyCode)   && !readOnly -> { Minecraft.getInstance().keyboardHandler.clipboard = value.selectedText; deleteSelected(value) }
                    !singleLine && !readOnly && event.keyCode in listOf(257, 335) ->
                        if (value.text.lines().size < maxLines) insert(value, "\n") else value
                    else -> { val after = handleMovementKey(event, value, singleLine, readOnly); if (after == value) handled = false; after }
                }
                if (result != value) onValueChangeAndScroll(result)
                if (handled || minecraftClient.options.keyInventory.matches(event.keyCode, 0)) event.consume(true)
            }
            .onCharTyped { _, event ->
                if (enabled && !readOnly && state.isFocused && StringUtil.isAllowedChatCharacter(event.codePoint)) {
                    if (value.text.length - value.selection.length < maxLength) {
                        onValueChangeAndScroll(insert(value, event.codePoint.toString())); event.consume(true)
                    }
                }
            }
            .onPointerEvent<LayoutNode>(PointerEventType.PRESS) { node, event ->
                if (state.isFocused && !node.isBounded(event.mouseX.toInt(), event.mouseY.toInt()))
                    state.onFocusChange(false)
            }
            .onPointerEvent<LayoutNode>(PointerEventType.PRESS) { node, event ->
                val (nX, _) = node.absoluteCoords
                val scrollBarX = nX + state.layoutInfo.width - 8
                if (!singleLine && event.mouseX >= scrollBarX && event.mouseX < nX + state.layoutInfo.width) {
                    state.isDraggingScrollbar = true
                } else {
                    state.onFocusChange(true)
                    val lX = event.mouseX - node.absoluteCoords.x - BORDER_PADDING
                    val lY = event.mouseY - node.absoluteCoords.y - BORDER_PADDING
                    val cur = findCursorPos(font, value.text, lX, lY, state, singleLine)
                    val sel = if (Screen.hasShiftDown()) TextRange(value.selection.end, cur) else TextRange(cur)
                    onValueChangeAndScroll(value.copy(selection = sel))
                }
                event.consume()
            }
            .onPointerEvent<LayoutNode>(PointerEventType.RELEASE) { _, _ -> state.isDraggingScrollbar = false }
            .onDrag<LayoutNode> { node, event ->
                if (state.isDraggingScrollbar) {
                    val contentH = value.text.lines().size * font.lineHeight
                    val innerH = state.layoutInfo.height - BORDER_PADDING * 2
                    val thumbH = Mth.clamp((innerH * innerH) / contentH, 32, innerH)
                    val maxScroll = (contentH - innerH).coerceAtLeast(1)
                    state.scrollY = Mth.clamp(state.scrollY + event.dragY * maxScroll.toDouble() / (innerH - thumbH), 0.0, maxScroll.toDouble())
                } else if (state.isFocused) {
                    val lX = event.mouseX - node.absoluteCoords.x - BORDER_PADDING
                    val lY = event.mouseY - node.absoluteCoords.y - BORDER_PADDING
                    val cur = findCursorPos(font, value.text, lX, lY, state, singleLine)
                    onValueChangeAndScroll(value.copy(selection = TextRange(value.selection.end, cur)))
                }
                event.consume()
            }
            .onScroll<LayoutNode> { _, event ->
                if (singleLine && !state.isFocused) return@onScroll
                val contentH = value.text.lines().size * font.lineHeight
                val innerH = state.layoutInfo.height - BORDER_PADDING * 2
                val maxScroll = (contentH - innerH).coerceAtLeast(0)
                state.scrollY = Mth.clamp(state.scrollY - event.scrollY * font.lineHeight / 2.0, 0.0, maxScroll.toDouble())
                event.consume()
            },
    ) { content(state) }
}

// ── Private helpers ────────────────────────────────────────────────────────

private fun findCursorPos(font: Font, text: String, x: Double, y: Double, state: TextFieldState, singleLine: Boolean): Int {
    return if (singleLine) {
        state.displayPos + font.plainSubstrByWidth(text.substring(state.displayPos), x.toInt().coerceAtLeast(0)).length
    } else {
        val scrolledY = y + state.scrollY
        val lineIdx = Mth.floor(scrolledY / font.lineHeight).coerceIn(0, text.lines().size - 1)
        val lineText = text.lines()[lineIdx]
        val charIdx = font.plainSubstrByWidth(lineText, x.toInt().coerceAtLeast(0)).length
        text.split('\n').take(lineIdx).sumOf { it.length + 1 } + charIdx
    }
}

private fun insert(value: TextFieldValue, text: String): TextFieldValue {
    val new = value.text.take(value.selection.min) + text + value.text.substring(value.selection.max)
    return TextFieldValue(new, TextRange(value.selection.min + text.length))
}

private fun deleteSelected(value: TextFieldValue): TextFieldValue {
    if (value.selection.length == 0) return value
    return TextFieldValue(value.text.take(value.selection.min) + value.text.substring(value.selection.max), TextRange(value.selection.min))
}

private fun handlePaste(value: TextFieldValue, maxLength: Int, maxLines: Int, singleLine: Boolean): TextFieldValue {
    var clip = Minecraft.getInstance().keyboardHandler.clipboard
    val avail = maxLength - (value.text.length - value.selection.length)
    if (clip.length > avail) clip = clip.take(avail)
    if (!singleLine) {
        val currentLines = value.text.lines().size
        val linesInSel = value.selectedText.count { it == '\n' }
        val availLines = maxLines - (currentLines - linesInSel)
        var nl = 0
        clip = buildString { for (c in clip) { if (c == '\n') { nl++; if (nl >= availLines) break }; append(c) } }
    }
    return insert(value, clip)
}

private fun handleMovementKey(event: KeyEvent, value: TextFieldValue, singleLine: Boolean, readOnly: Boolean): TextFieldValue {
    if (readOnly) return when (event.keyCode) {
        262, 263, 264, 265, 268, 269 -> moveKey(event, value, singleLine)
        else -> value
    }
    return moveKey(event, value, singleLine)
}

private fun moveKey(event: KeyEvent, value: TextFieldValue, singleLine: Boolean): TextFieldValue {
    val shift = Screen.hasShiftDown(); val ctrl = Screen.hasControlDown()
    val text = value.text; val sel = value.selection
    return when (event.keyCode) {
        259 -> { // BACKSPACE
            if (sel.length > 0) deleteSelected(value)
            else if (sel.start == 0) value
            else { val p = if (ctrl) findLastWord(text, sel.start) else sel.start - 1; TextFieldValue(text.take(p) + text.substring(sel.start), TextRange(p)) }
        }
        261 -> { // DELETE
            if (sel.length > 0) deleteSelected(value)
            else if (sel.start == text.length) value
            else { val p = if (ctrl) findNextWord(text, sel.start) else sel.start + 1; TextFieldValue(text.take(sel.start) + text.substring(p), TextRange(sel.start)) }
        }
        263 -> { // LEFT
            val p = if (ctrl) findLastWord(text, sel.start) else (sel.start - 1).coerceAtLeast(0)
            value.copy(selection = if (shift) TextRange(sel.end, p) else TextRange(p))
        }
        262 -> { // RIGHT
            val p = if (ctrl) findNextWord(text, sel.start) else (sel.start + 1).coerceAtMost(text.length)
            value.copy(selection = if (shift) TextRange(sel.end, p) else TextRange(p))
        }
        265 -> if (!singleLine) moveVertical(value, -1, shift) else value  // UP
        264 -> if (!singleLine) moveVertical(value, 1,  shift) else value  // DOWN
        268 -> { // HOME
            val nl = text.take(sel.start).lastIndexOf('\n')
            val ls = if (nl == -1) 0 else nl + 1
            value.copy(selection = if (shift) TextRange(sel.end, ls) else TextRange(ls))
        }
        269 -> { // END
            val nl = text.indexOf('\n', sel.start)
            val le = if (nl == -1) text.length else nl
            value.copy(selection = if (shift) TextRange(sel.end, le) else TextRange(le))
        }
        else -> value
    }
}

private fun moveVertical(value: TextFieldValue, delta: Int, select: Boolean): TextFieldValue {
    val lines = value.text.lines(); val cur = value.selection.start
    val curLine = value.text.take(cur).count { it == '\n' }
    val target = (curLine + delta).coerceIn(0, lines.lastIndex)
    if (curLine == target) return value
    val lastNl = value.text.take(cur).lastIndexOf('\n')
    val col = cur - (if (lastNl == -1) 0 else lastNl + 1)
    val tStart = value.text.split('\n').take(target).sumOf { it.length + 1 }
    val newPos = (tStart + col).coerceAtMost(tStart + lines[target].length)
    return value.copy(selection = if (select) TextRange(value.selection.end, newPos) else TextRange(newPos))
}

private fun findNextWord(text: String, from: Int): Int {
    var i = from; while (i < text.length && text[i] == ' ') i++; while (i < text.length && text[i] != ' ') i++; return i
}
private fun findLastWord(text: String, from: Int): Int {
    var i = from - 1; while (i >= 0 && text[i] == ' ') i--; while (i >= 0 && text[i] != ' ') i--; return i + 1
}
