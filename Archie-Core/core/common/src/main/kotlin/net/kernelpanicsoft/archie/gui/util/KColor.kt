package net.kernelpanicsoft.archie.gui.util

import kotlinx.serialization.Serializable
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.TextColor
import net.minecraft.util.Mth
import net.minecraft.world.item.DyeColor
import kotlin.random.Random

/**
 * A serializable, immutable ARGB colour value for use in GUI composables.
 *
 * [KColor] is a pure-Kotlin alternative to [java.awt.Color] that works with
 * Minecraft's integer-packed colour conventions. All component values are in
 * the range 0–255.
 *
 * ### Creating colours
 * ```kotlin
 * val red    = KColor.RED
 * val custom = KColor.ofRgb(0xFF8C00)          // opaque dark orange
 * val semi   = KColor.ofArgb(0x80FF0000L)       // 50 % transparent red
 * val hsv    = KColor.ofHsv(0.33f, 1f, 0.8f)   // dark green via HSV
 * ```
 *
 * @property red   The red channel (0–255).
 * @property green The green channel (0–255).
 * @property blue  The blue channel (0–255).
 * @property alpha The alpha channel (0–255, where 0 is fully transparent and 255 is opaque).
 */
@Serializable
data class KColor(
    val red: Int = 0,
    val green: Int = 0,
    val blue: Int = 0,
    val alpha: Int = 255,
) {
    companion object {
        // ── Predefined colours ──────────────────────────────────
        val WHITE      = ofRgb(0xFFFFFF)
        val LIGHT_GRAY = ofRgb(0xC0C0C0)
        val GRAY       = ofRgb(0x808080)
        val DARK_GRAY  = ofRgb(0x404040)
        val BLACK      = ofRgb(0x000000)
        val RED        = ofRgb(0xFF0000)
        val PINK       = ofRgb(0xFFAFAF)
        val ORANGE     = ofRgb(0xFFA500)
        val YELLOW     = ofRgb(0xFFFF00)
        val GREEN      = ofRgb(0x4CAF50)
        val MAGENTA    = ofRgb(0xFF00FF)
        val CYAN       = ofRgb(0x00FFFF)
        val LIGHT_BLUE = ofRgb(0x2196F3)
        val BLUE       = ofRgb(0x0000FF)

        /**
         * Creates a [KColor] from a packed ARGB [Long] in the form `0xAARRGGBB`.
         *
         * @param argb The packed ARGB value.
         */
        fun ofArgb(argb: Long): KColor = KColor(
            red   = (argb shr 16 and 255).toInt(),
            green = (argb shr 8  and 255).toInt(),
            blue  = (argb        and 255).toInt(),
            alpha = (argb shr 24).toInt(),
        )

        /**
         * Creates an opaque [KColor] from a packed RGB [Int] in the form `0xRRGGBB`.
         *
         * @param rgb The packed RGB value (alpha is set to 255).
         */
        fun ofRgb(rgb: Int): KColor = KColor(
            red   = rgb shr 16 and 255,
            green = rgb shr 8  and 255,
            blue  = rgb        and 255,
        )

        /**
         * Creates a [KColor] from HSV (Hue, Saturation, Value) components with full opacity.
         *
         * @param hue        Hue in the range [0, 1].
         * @param saturation Saturation in the range [0, 1].
         * @param value      Value (brightness) in the range [0, 1].
         */
        fun ofHsv(hue: Float, saturation: Float, value: Float): KColor =
            ofRgb(Mth.hsvToRgb(hue - 0.5e-7f, saturation, value))

        /**
         * Creates a [KColor] from HSV components with a custom alpha.
         *
         * @param hue        Hue in the range [0, 1].
         * @param saturation Saturation in the range [0, 1].
         * @param value      Value (brightness) in the range [0, 1].
         * @param alpha      Alpha in the range [0, 1] (0 = fully transparent, 1 = opaque).
         */
        fun ofHsv(hue: Float, saturation: Float, value: Float, alpha: Float): KColor =
            ofArgb(((alpha * 255).toInt().toLong() shl 24 or Mth.hsvToRgb(hue - 0.5e-7f, saturation, value).toLong()))

        /**
         * Creates a [KColor] from the colour associated with a [ChatFormatting] constant.
         *
         * @param formatting A [ChatFormatting] value with an associated colour.
         */
        fun ofFormatting(formatting: ChatFormatting): KColor = ofRgb(formatting.color ?: 0)

        /**
         * Creates a [KColor] from a Minecraft [DyeColor].
         *
         * @param dye The dye colour.
         */
        fun ofDye(dye: DyeColor): KColor = ofArgb(dye.textureDiffuseColor.toLong())

        /**
         * Generates a random [KColor].
         *
         * @param alpha Whether the alpha channel should also be randomized. When `false` the
         *   colour is fully opaque.
         */
        fun random(alpha: Boolean = true): KColor =
            if (alpha) ofArgb(Random.nextLong(0x100000000) or 0xFF000000L)
            else ofRgb(Random.nextInt(0x1000000))
    }

    /**
     * The packed RGB integer representation (no alpha channel, in the form `0xRRGGBB`).
     */
    val rgb: Int get() = (red shl 16) or (green shl 8) or blue

    /**
     * The packed ARGB integer representation (in the form `0xAARRGGBB`).
     */
    val argb: Int get() = (alpha shl 24) or (red shl 16) or (green shl 8) or blue

    /** Converts this to a vanilla [TextColor] (RGB only - [TextColor] carries no alpha channel). */
    fun toTextColor(): TextColor = TextColor.fromRgb(rgb)
}
