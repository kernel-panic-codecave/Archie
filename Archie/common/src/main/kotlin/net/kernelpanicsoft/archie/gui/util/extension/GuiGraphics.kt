package net.kernelpanicsoft.archie.gui.util.extension

import com.mojang.blaze3d.vertex.PoseStack
import net.kernelpanicsoft.archie.gui.layout.IntRect
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

/** Fills a rectangle with a 4-corner color gradient using the default GUI [RenderType]. */
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

/**
 * Fills a rectangle with a 4-corner color gradient, unlike vanilla's [GuiGraphics.fillGradient]
 * (top-to-bottom only), by directly emitting one quad with a per-vertex ARGB color to [type].
 */
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

/** Draws an unfilled rectangle outline of [thickness] pixels using the default GUI [RenderType]. */
fun GuiGraphics.drawRectOutline(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    color: Int,
    thickness: Int = 1,
) = drawRectOutline(RenderType.gui(), x, y, width, height, color, thickness)

/** Draws an unfilled rectangle outline of [thickness] pixels as four filled edge strips. */
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

/**
 * Runs [block] with this [GuiGraphics]'s [PoseStack][com.mojang.blaze3d.vertex.PoseStack] pushed,
 * popping it again afterwards (including when [block] throws). Saves the manual
 * `pose().pushPose()` / `pose().popPose()` pairing renderers otherwise need around
 * translate/scale/rotate calls.
 */
fun <T> GuiGraphics.pose(block: PoseStack.() -> T): T
{
    val pose = pose()
    pose.pushPose()
    val ret  = pose.block()
    pose.popPose()
    return ret
}

/**
 * Runs [block] with scissoring enabled to the `[minX, minY, maxX, maxY)` rectangle, disabling
 * it again afterwards. Saves the manual `enableScissor(...)` / `disableScissor()` pairing
 * renderers otherwise need around clipped content.
 */
fun <T> GuiGraphics.scissor(minX: Int, minY: Int, maxX: Int, maxY: Int, block: () -> T): T
{
    enableScissor(minX, minY, maxX, maxY)
    val ret = block()
    disableScissor()
    return ret
}

/** Overload of [scissor] taking the clip bounds as an [IntRect]. */
fun <T> GuiGraphics.scissor(rect: IntRect, block: () -> T): T
{
    val (minX: Int, minY: Int, maxX: Int, maxY: Int) = rect
    enableScissor(minX, minY, maxX, maxY)
    val ret = block()
    disableScissor()
    return ret
}

/**
 * Lets a [GuiGraphics] receiver be invoked like `guiGraphics { ... }`, running [block] with
 * `this` as the receiver. Used throughout the built-in composables' `Renderer` implementations
 * to avoid repeating the `guiGraphics.` prefix on every draw call.
 */
operator fun GuiGraphics.invoke(block: GuiGraphics.() -> Unit): Unit = block()
