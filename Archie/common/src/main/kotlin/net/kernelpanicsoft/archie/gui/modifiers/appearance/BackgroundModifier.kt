package net.kernelpanicsoft.archie.gui.modifiers.appearance

import androidx.compose.runtime.Stable
import net.kernelpanicsoft.archie.gui.modifiers.ContentDrawScope
import net.kernelpanicsoft.archie.gui.modifiers.DrawModifier
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.util.KColor
import net.kernelpanicsoft.archie.gui.util.extension.fillGradient

/**
 * The direction along which a background gradient transitions.
 */
enum class GradientDirection {
    TOP_TO_BOTTOM,
    RIGHT_TO_LEFT,
    LEFT_TO_RIGHT,
    BOTTOM_TO_TOP,
}

/**
 * A [DrawModifier] that fills a composable's background with a solid colour or a two-stop
 * linear gradient.
 *
 * When [startColor] and [endColor] are equal the fill is solid; otherwise the two colours
 * are interpolated across the node bounds in [gradientDirection].
 *
 * @property startColor       ARGB packed start colour.
 * @property endColor         ARGB packed end colour.
 * @property gradientDirection The direction of the gradient transition.
 */
data class BackgroundModifier(
    val startColor: Int,
    val endColor: Int,
    val gradientDirection: GradientDirection = GradientDirection.TOP_TO_BOTTOM,
) : Modifier.Element<BackgroundModifier>, DrawModifier {

    override fun ContentDrawScope.draw() {
        val (topLeft, topRight, bottomLeft, bottomRight) = when (gradientDirection) {
            GradientDirection.TOP_TO_BOTTOM -> listOf(startColor, startColor, endColor,   endColor)
            GradientDirection.BOTTOM_TO_TOP -> listOf(endColor,   endColor,   startColor, startColor)
            GradientDirection.LEFT_TO_RIGHT -> listOf(startColor, endColor,   startColor, endColor)
            GradientDirection.RIGHT_TO_LEFT -> listOf(endColor,   startColor, endColor,   startColor)
        }
        guiGraphics.fillGradient(x, y, width, height, topLeft, topRight, bottomLeft, bottomRight)
        drawContent()
    }

    override fun mergeWith(other: BackgroundModifier): BackgroundModifier = other

    override fun toString(): String =
        if (startColor == endColor)
            "BackgroundModifier(color=#${String.format("%08X", startColor)})"
        else
            "BackgroundModifier(startColor=#${String.format("%08X", startColor)}, endColor=#${String.format("%08X", endColor)}, direction=$gradientDirection)"
}

/**
 * Fills the composable's background with a solid [color].
 */
@Stable fun Modifier.background(color: KColor): Modifier =
    this then BackgroundModifier(color.argb, color.argb)

/**
 * Fills the composable's background with a gradient from [startColor] to [endColor]
 * going top-to-bottom.
 */
@Stable fun Modifier.background(startColor: KColor, endColor: KColor): Modifier =
    this then BackgroundModifier(startColor.argb, endColor.argb)

/**
 * Fills the composable's background with a gradient from [startColor] to [endColor]
 * in the given [gradientDirection].
 */
@Stable fun Modifier.background(
    startColor: KColor,
    endColor: KColor,
    gradientDirection: GradientDirection = GradientDirection.TOP_TO_BOTTOM,
): Modifier = this then BackgroundModifier(startColor.argb, endColor.argb, gradientDirection)

/** Fills the composable's background with a solid ARGB integer [color]. */
@Stable fun Modifier.background(color: Int): Modifier =
    this then BackgroundModifier(color, color)

/** Fills the composable's background with a gradient between two ARGB integer colours. */
@Stable fun Modifier.background(startColor: Int, endColor: Int): Modifier =
    this then BackgroundModifier(startColor, endColor)

/** Fills the composable's background with a directional gradient between two ARGB integer colours. */
@Stable fun Modifier.background(
    startColor: Int,
    endColor: Int,
    gradientDirection: GradientDirection = GradientDirection.TOP_TO_BOTTOM,
): Modifier = this then BackgroundModifier(startColor, endColor, gradientDirection)
