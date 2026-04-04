package net.kernelpanicsoft.archie.gui.util.extension

import net.kernelpanicsoft.archie.gui.theme.SimpleThemeState
import net.kernelpanicsoft.archie.gui.theme.ThemeState
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation

private fun ResourceLocation.isAtlasSprite(): Boolean =
    !path.startsWith("textures/") && !path.endsWith(".png")

/** Draws a ThemeState to the screen. */
fun GuiGraphics.drawThemeState(state: ThemeState, x: Int, y: Int, width: Int, height: Int) {
    state as SimpleThemeState
    if (state.texture.isAtlasSprite()) {
        blitSprite(state.texture, x, y, width, height)
    } else {
        blit(state, x, y)
    }
}

/** A helper extension to blit a SimpleThemeState without manually extracting all its properties. */
fun GuiGraphics.blit(state: SimpleThemeState, x: Int, y: Int) {
    this.blit(
        state.texture,
        x,
        y,
        state.width,
        state.height,
        state.u.toFloat(),
        state.v.toFloat(),
        state.uWidth,
        state.vHeight,
        state.textureSize.width,
        state.textureSize.height,
    )
}

fun GuiGraphics.fillGradient(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    topLeftColor: Int,
    topRightColor: Int,
    bottomLeftColor: Int,
    bottomRightColor: Int,
) = fillGradient(
    RenderType.gui(),
    x,
    y,
    width,
    height,
    topLeftColor,
    topRightColor,
    bottomLeftColor,
    bottomRightColor,
)

fun GuiGraphics.fillGradient(
    type: RenderType,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    topLeftColor: Int,
    topRightColor: Int,
    bottomLeftColor: Int,
    bottomRightColor: Int,
) {
    val buffer = bufferSource().getBuffer(type)
    val matrix = pose().last().pose()

    buffer.addVertex(matrix, x + width, y, 0).setColor(topRightColor)
    buffer.addVertex(matrix, x, y, 0).setColor(topLeftColor)
    buffer.addVertex(matrix, x, y + height, 0).setColor(bottomLeftColor)
    buffer.addVertex(matrix, x + width, y + height, 0).setColor(bottomRightColor)
}

fun GuiGraphics.drawRectOutline(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    color: Int,
    thickness: Int = 1,
) = drawRectOutline(RenderType.gui(), x, y, width, height, color, thickness)

fun GuiGraphics.drawRectOutline(
    type: RenderType,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    color: Int,
    thickness: Int = 1,
) {
    fill(type, x, y, x + width, y + thickness, color)
    fill(type, x, y + height - thickness, x + width, y + height, color)

    fill(type, x, y + thickness, x + thickness, y + height - thickness, color)
    fill(type, x + width - thickness, y + thickness, x + width, y + height - thickness, color)
}
