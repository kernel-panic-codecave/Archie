package net.kernelpanicsoft.archie.gui.util

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * An immutable colour representation using the Hue-Saturation-Value model with an alpha channel.
 *
 * This representation is primarily intended for use with [net.kernelpanicsoft.archie.gui.composables.input.ColorPicker],
 * which operates natively in HSV space to avoid lossy round-trip conversions.
 *
 * All component values are in the range **[0.0, 1.0]**.
 *
 * ### Example
 * ```kotlin
 * val red      = HsvColor(hue = 0f,    saturation = 1f, value = 1f, alpha = 1f)
 * val fromKColor = HsvColor.from(KColor.CYAN)
 * val backToKColor = red.toKColor()
 * ```
 *
 * @property hue        The hue angle normalized to [0, 1] (0 and 1 both represent red).
 * @property saturation The saturation (0 = grey, 1 = fully saturated).
 * @property value      The brightness value (0 = black, 1 = maximum brightness).
 * @property alpha      The alpha transparency (0 = fully transparent, 1 = fully opaque).
 */
@Immutable
data class HsvColor(
    val hue: Float,
    val saturation: Float,
    val value: Float,
    val alpha: Float,
) {
    /**
     * Converts this HSV colour to an equivalent [KColor] (ARGB).
     */
    fun toKColor(): KColor = KColor.ofHsv(hue, saturation, value, alpha)

    companion object {
        /**
         * Creates an [HsvColor] from an existing [KColor] by converting its RGB components
         * to HSV using the JVM's [java.awt.Color.RGBtoHSB] utility.
         *
         * @param color The source [KColor] to convert.
         */
        fun from(color: KColor): HsvColor {
            val hsb = java.awt.Color.RGBtoHSB(color.red, color.green, color.blue, null)
            return HsvColor(
                hue        = hsb[0],
                saturation = hsb[1],
                value      = hsb[2],
                alpha      = color.alpha / 255f,
            )
        }
    }
}
