package net.kernelpanicsoft.archie.gui.composables.input.textfield

import androidx.compose.runtime.Immutable
import kotlin.math.max
import kotlin.math.min

/**
 * Represents a half-open character range `[start, end)` within a text field's content string.
 *
 * When [start] == [end] the range is *collapsed* and represents a cursor position rather than
 * a selection. [start] and [end] may be in either order; [min] and [max] always give the
 * canonical inclusive/exclusive bounds regardless of direction.
 *
 * @property start The anchor end of the range (inclusive, 0-based character index).
 * @property end   The active end of the range. Defaults to [start] (collapsed cursor).
 */
@Immutable
data class TextRange(val start: Int, val end: Int = start) {
    /** `true` when [start] and [end] are equal (cursor, no selection). */
    val isCollapsed: Boolean get() = start == end

    /** The number of characters in the selected range. */
    val length: Int get() = max(start, end) - min(start, end)

    /** The smaller of [start] and [end] — inclusive start of the selected region. */
    val min: Int get() = minOf(start, end)

    /** The larger of [start] and [end] — exclusive end of the selected region. */
    val max: Int get() = maxOf(start, end)

    companion object {
        /** A collapsed [TextRange] positioned at the beginning of the string. */
        val Zero = TextRange(0)
    }

    override fun toString(): String = "TextRange(start=$start, end=$end)"
}

/**
 * Immutable value holder for a [net.kernelpanicsoft.archie.gui.composables.input.textfield.TextField]
 * or [BasicTextField].
 *
 * Contains the full text, the current selection (or cursor position), and an optional IME
 * composition range. Pass new instances to `onValueChange` to update the field.
 *
 * @property text        The current text content.
 * @property selection   The current selection or cursor position within [text].
 * @property composition The active IME composition range, or `null` when no composition is in progress.
 */
@Immutable
data class TextFieldValue(
    val text: String = "",
    val selection: TextRange = TextRange(text.length),
    val composition: TextRange? = null,
) {
    /** The characters currently selected by the user (empty string when the selection is collapsed). */
    val selectedText: String get() = text.substring(selection.min, selection.max)

    override fun toString(): String =
        "TextFieldValue(text='$text', selection=$selection, composition=$composition)"
}
