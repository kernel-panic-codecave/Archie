package net.kernelpanicsoft.archie.gui.composables.basic

import androidx.compose.runtime.Composable
import net.kernelpanicsoft.archie.gui.layout.Layout
import net.kernelpanicsoft.archie.gui.layout.LayoutNode
import net.kernelpanicsoft.archie.gui.layout.MeasureResult
import net.kernelpanicsoft.archie.gui.layout.Size
import net.kernelpanicsoft.archie.gui.modifiers.Modifier
import net.kernelpanicsoft.archie.gui.modifiers.SizeModifier
import net.kernelpanicsoft.archie.gui.modifiers.Constraints
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component

/**
 * Returns the rendered pixel size (width × height) of [text] at the given [scale].
 *
 * Useful for sizing containers to exactly fit their text content before composition.
 *
 * @param text  The [Component] whose rendered dimensions are measured.
 * @param scale Font scale factor (1.0 = native size).
 * @param font  The [Font] to measure with; defaults to Minecraft's standard font.
 */
fun getTextSize(
    text: Component,
    scale: Float = 1f,
    font: Font = Minecraft.getInstance().font,
): Size = Size((font.width(text) * scale).toInt(), (font.lineHeight * scale).toInt())

/**
 * Renders a [Component] using Minecraft's font renderer.
 *
 * Supports optional uniform scaling, a custom font face, and an ARGB text colour.
 * The node automatically sizes itself to the minimum dimensions needed to display the
 * text at the requested scale.
 *
 * ### Example
 * ```kotlin
 * Text(
 *     text = Component.literal("Hello, Archie!"),
 *     fontScale = 1.5f,
 *     color = KColor.YELLOW.argb,
 * )
 * ```
 *
 * @param text      The text component to render.
 * @param fontScale Uniform scale applied to the font. Default `1f` (native size).
 * @param font      The [Font] used for rendering and size measurement.
 * @param color     ARGB packed text colour. Default opaque white (`0xFFFFFFFF`).
 * @param modifier  Additional modifiers applied to the layout node.
 */
@Composable
fun Text(
    text: Component,
    fontScale: Float = 1f,
    font: Font = Minecraft.getInstance().font,
    color: Int = 0xFFFFFFFF.toInt(),
    modifier: Modifier = Modifier,
) {
    val textSize = getTextSize(text, fontScale, font)
    Layout(
        measurePolicy = { _, _, constraints ->
            MeasureResult(constraints.minWidth, constraints.minHeight) {}
        },
        renderer = object : net.kernelpanicsoft.archie.gui.layout.Renderer {
            override fun render(
                node: net.kernelpanicsoft.archie.gui.nodes.AUINode,
                x: Int, y: Int,
                guiGraphics: GuiGraphics,
                mouseX: Int, mouseY: Int,
                partialTick: Float,
            ) {
                if (fontScale != 1f) {
                    guiGraphics.pose().apply {
                        pushPose()
                        scale(fontScale, fontScale, fontScale)
                        translate(x / fontScale, y / fontScale, 0f)
                    }
                    guiGraphics.drawString(font, text, 0, 0, color)
                    guiGraphics.pose().popPose()
                } else {
                    guiGraphics.drawString(font, text, x, y, color)
                }
                super.render(node, x, y, guiGraphics, mouseX, mouseY, partialTick)
            }
        },
        modifier = Modifier.then(
            SizeModifier(Constraints(textSize.width, textSize.width, textSize.height, textSize.height))
        ).then(modifier),
    )
}
