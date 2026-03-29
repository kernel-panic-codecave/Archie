package net.kernelpanicsoft.archie.gui.modifiers.appearance

import androidx.compose.runtime.Stable
import net.kernelpanicsoft.archie.gui.modifiers.ContentDrawScope
import net.kernelpanicsoft.archie.gui.modifiers.DrawModifier
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.util.KColor
import net.kernelpanicsoft.archie.gui.util.drawRectOutline

/**
 * A [DrawModifier] that draws a rectangular border around a composable.
 *
 * The border is rendered **before** the composable's own content so that it appears
 * underneath any child nodes.
 *
 * @property color     ARGB packed border colour.
 * @property thickness Border stroke width in pixels.
 */
data class BorderModifier(
    val color: Int,
    val thickness: Int,
) : Modifier.Element<BorderModifier>, DrawModifier {

    override fun mergeWith(other: BorderModifier): BorderModifier = other

    override fun ContentDrawScope.draw() {
        guiGraphics.drawRectOutline(x, y, width, height, color, thickness)
        drawContent()
    }

    override fun toString(): String =
        "BorderModifier(width=$thickness, color=#${String.format("%08X", color)})"
}

/**
 * Adds a border of [thickness] pixels and [color] to the composable.
 *
 * @param color     The border colour.
 * @param thickness The border stroke width in pixels (default 1).
 */
@Stable
fun Modifier.border(color: KColor, thickness: Int = 1): Modifier =
    this then BorderModifier(color.argb, thickness)

/**
 * Adds a border using a raw ARGB integer [color].
 *
 * @param color     ARGB packed colour.
 * @param thickness The border stroke width in pixels (default 1).
 */
@Stable
fun Modifier.border(color: Int, thickness: Int = 1): Modifier =
    this then BorderModifier(color, thickness)
