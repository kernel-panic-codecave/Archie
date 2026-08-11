package net.kernelpanicsoft.archie.gui.modifiers.appearance

import androidx.compose.runtime.State
import androidx.compose.runtime.Stable
import net.kernelpanicsoft.archie.gui.modifiers.ContentDrawScope
import net.kernelpanicsoft.archie.gui.modifiers.DrawModifier
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.util.KColor
import net.kernelpanicsoft.archie.gui.util.extension.drawRectOutline

/**
 * A [DrawModifier] that outlines a composable while [focused] is `true` - the visible
 * counterpart to [net.kernelpanicsoft.archie.gui.modifiers.input.focusable], giving
 * keyboard/controller users the same "what's focused" feedback vanilla widgets draw for free.
 *
 * Drawn *after* the node's own content (unlike [BorderModifier], which draws first) so the
 * ring sits on top rather than being obscured by the content it's outlining.
 *
 * @property color     ARGB packed ring colour.
 * @property thickness Ring stroke width in pixels.
 */
data class FocusRingModifier(
    val focused: State<Boolean>,
    val color: Int,
    val thickness: Int,
) : Modifier.Element<FocusRingModifier>, DrawModifier {

    override fun mergeWith(other: FocusRingModifier): FocusRingModifier = other

    override fun ContentDrawScope.draw() {
        drawContent()
        if (focused.value) guiGraphics.drawRectOutline(x, y, width, height, color, thickness)
    }

    override fun toString(): String = "FocusRingModifier(focused=${focused.value})"
}

/**
 * Draws a [color] outline around this composable while [focused] is `true`.
 *
 * @param color     The ring colour.
 * @param thickness The ring stroke width in pixels (default 1).
 */
@Stable
fun Modifier.focusRing(focused: State<Boolean>, color: KColor = KColor.YELLOW, thickness: Int = 1): Modifier =
    this then FocusRingModifier(focused, color.argb, thickness)

/**
 * Draws an outline around this composable while [focused] is `true`, using a raw ARGB [color].
 *
 * @param thickness The ring stroke width in pixels (default 1).
 */
@Stable
fun Modifier.focusRing(focused: State<Boolean>, color: Int, thickness: Int = 1): Modifier =
    this then FocusRingModifier(focused, color, thickness)
